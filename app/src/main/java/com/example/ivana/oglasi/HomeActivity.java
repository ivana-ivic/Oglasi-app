package com.example.ivana.oglasi;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.Revision;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.example.ivana.oglasi.Classes.DatabaseInstance;
import com.example.ivana.oglasi.Classes.DropboxCredentials;
import com.example.ivana.oglasi.Classes.Helper;
import com.example.ivana.oglasi.Classes.HomeListAdapter;
import com.example.ivana.oglasi.Classes.HomeListItemData;
import com.example.ivana.oglasi.Classes.ImageSaver;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class HomeActivity extends AppCompatActivity {

    public static final String TAG = "Oglasi";
    public static AndroidContext androidContext;
    URL url;
    Replication pull;
    HomeListAdapter homeListAdapter;
    ListView homeListView;
    ProgressDialog progressDialog;
    String lastLoadedAdId;
    boolean footerAdded=false;
    Button mLoadMore;
    boolean loadedMore=false;
    Replication pullUser;
    int adsLoadedCounter=0;
    ArrayList<String> ids=new ArrayList<>();
    boolean userLoaded=false;
    boolean initialAdsLoading=true;
    FloatingActionButton adsToTop;
    boolean deletedAdsLoaded=false;
    Replication deletedAdsPull;
    ArrayList<String> deletedIds;
    boolean thumbnailsDownloaded=false;
    CloudStorage dropbox;
    ProgressBar homeLoading;
    boolean userPulled=false;
    int adsCount=0;
    boolean refresh=false;

    public BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean result=intent.getBooleanExtra("counterChanged",false);
            if(result){
                ids.clear();
                ids.addAll((ArrayList<String>)intent.getExtras().get("ids"));
                adsCount=ids.size();

                new ResolveAdCounterConflictsTask().execute();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        invalidateOptionsMenu();

        CloudRail.setAppKey(DropboxCredentials.AppKey);
        dropbox = new Dropbox(getApplicationContext(), DropboxCredentials.API_ID, DropboxCredentials.API_KEY);

        androidContext = new AndroidContext(this);

        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(refresh){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adsToTop.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }, 0, 60000);

        adsToTop=(FloatingActionButton)findViewById(R.id.floatingActionButton_adsToTop);
        adsToTop.setVisibility(View.GONE);
        adsToTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeLoading.setVisibility(View.VISIBLE);
                adsToTop.setVisibility(View.GONE);
                adsLoadedCounter=0;
                homeListAdapter=new HomeListAdapter(getApplicationContext(),R.layout.home_list_item);
                footerAdded=false;
                loadedMore=false;
                homeListView.removeFooterView(mLoadMore);
                ignoreDeletedAds();
                lastLoadedAdId=startAdIndex();
                tryAdsPull();
            }
        });

        homeListAdapter=new HomeListAdapter(getApplicationContext(),R.layout.home_list_item);
        homeListView=(ListView)findViewById(R.id.listView_ads);
        homeLoading=(ProgressBar)findViewById(R.id.progressBar_homeLoading);

        mLoadMore=new Button(HomeActivity.this);
        mLoadMore.setId(R.id.id_load_more_button);
        String btnText="Učitaj još";
        mLoadMore.setText(btnText);
        mLoadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadedMore=true;
                if(Helper.isNetworkAvailable(getApplicationContext())){
                    homeLoading.setVisibility(View.VISIBLE);
                    tryAdsPull();
                }
                else{
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(HomeActivity.this);
                    builder1.setMessage("Morate biti povezani na internet da biste učitali još oglasa.");
                    builder1.setCancelable(true);

                    builder1.setNeutralButton(
                            "Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                }
            }
        });

        try {
            url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
            pull = DatabaseInstance.getInstance().database.createPullReplication(url);
            pull.setContinuous(false);
            Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
            pull.setAuthenticator(auth);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        pull.addChangeListener(new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
                boolean active = (pull.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                if (!active) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                        }
                    });

                    homeListAdapter=new HomeListAdapter(getApplicationContext(),R.layout.home_list_item);
                    if(Helper.isNetworkAvailable(getApplicationContext())){
                        new Thread(){
                            @Override
                            public void run(){
                                for(int i=ids.size()-1-adsLoadedCounter+10;i>=ids.size()-adsLoadedCounter;i--){
                                    Document ad=DatabaseInstance.getInstance().database.getExistingDocument(ids.get(i));
                                    if(ad!=null){
                                        Helper.resolveAdConflicts(ids.get(i));
                                        ad=DatabaseInstance.getInstance().database.getExistingDocument(ids.get(i));
                                        Map<String,Object> adProps=new HashMap();
                                        adProps.putAll(ad.getProperties());
                                        List<String> adImageLinks=(ArrayList<String>)adProps.get("images");
                                        if(adImageLinks.size()!=0){
                                            Bitmap bm;
                                            String[] folderAndName=adImageLinks.get(0).split("/");
                                            String path=folderAndName[0]+"/thumbnail_"+folderAndName[1];
                                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), path);
                                            if(!file.exists()){
                                                if(dropbox.exists("/Oglasi/"+adImageLinks.get(0))){
                                                    InputStream result=dropbox.getThumbnail("/Oglasi/"+adImageLinks.get(0));
                                                    bm = BitmapFactory.decodeStream(result);
                                                    new ImageSaver(getApplicationContext()).setExternal(true).setDirectoryName(folderAndName[0]).setFileName("thumbnail_"+folderAndName[1]).save(bm);
                                                }
                                            }
                                        }
                                    }
                                }
                                thumbnailsDownloaded=true;
                            }
                        }.start();
                    }
                    loadAds();
                    if(!isMyServiceRunning(ContinuousPushService.class)){
                        Intent startPush=new Intent(getBaseContext(),ContinuousPushService.class);
                        startService(startPush);
                    }
                } else {
                    double total = pull.getCompletedChangesCount();
                    progressDialog.setMax((int) total);
                    progressDialog.setProgress(pull.getChangesCount());
                }
            }
        });

        if(!Helper.isNetworkAvailable(getApplicationContext())){
            if(DatabaseInstance.getInstance().database.getDocumentCount()>1){
                Document adsCounter=DatabaseInstance.getInstance().database.getExistingDocument("ads_counter");
                Map<String,Object> counterProps=new HashMap<String, Object>();
                counterProps.putAll(adsCounter.getProperties());
                ids=(ArrayList<String>)counterProps.get("ids");

                ArrayList<String> viewAdIds=new ArrayList<>();
                Query queryData = DatabaseInstance.getInstance().adsView().createQuery();
                queryData.setMapOnly(true);
                queryData.setDescending(false);
                try{
                    QueryEnumerator result = queryData.run();
                    for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                        QueryRow row = it.next();
                        viewAdIds.add(row.getKey().toString());
                    }
                }catch(CouchbaseLiteException e){
                    Log.e("Oglasi",e.getMessage());
                }

                for(int i=0;i<ids.size();i++){
                    String adId=ids.get(i);
                    if(!viewAdIds.contains(adId)){
                        ids.remove(adId);
                        i--;
                    }
                }

                lastLoadedAdId=ids.get(ids.size()-1);
                adsLoadedCounter=ids.size();

                this.loadAds();
            }
            else{
                homeLoading.setVisibility(View.GONE);
                AlertDialog.Builder builder1 = new AlertDialog.Builder(HomeActivity.this);
                builder1.setTitle("Nema podataka");
                builder1.setMessage("Podaci ne postoje u telefonu. Povežite se na internet kako bi učitali podatke.");
                builder1.setCancelable(true);

                builder1.setNeutralButton(
                        "Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert11 = builder1.create();
                alert11.show();
            }
        }
        else{
            if(!isMyServiceRunning(AdCounterPullService.class)){
                Intent startPullingCounter=new Intent(getBaseContext(),AdCounterPullService.class);
                startService(startPullingCounter);
            }

            try{
                URL url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                final Replication pullDeletedUsers = DatabaseInstance.getInstance().database.createPullReplication(url);
                pullDeletedUsers.setContinuous(false);
                Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                pullDeletedUsers.setAuthenticator(auth);
                List<String> docIds=new ArrayList<>();
                docIds.add("users_deleted");
                pullDeletedUsers.setDocIds(docIds);
                pullDeletedUsers.start();

                pullDeletedUsers.addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(Replication.ChangeEvent event) {
                        boolean active=pullDeletedUsers.getStatus()== Replication.ReplicationStatus.REPLICATION_ACTIVE;
                        if(!active){
//                            deletedUsersListPulled=true;
                            Helper.resolveDeletedUsersListConflicts();
                            Document deletedUsersDoc=DatabaseInstance.getInstance().database.getExistingDocument("users_deleted");
                            Map<String,Object> deletedUsersProps=new HashMap<>();
                            deletedUsersProps.putAll(deletedUsersDoc.getProperties());
                            ArrayList<String> deletedUsernames=(ArrayList<String>)deletedUsersProps.get("usernames");

                            try{
                                for(int i=0;i<deletedUsernames.size();i++){
                                    Document user=DatabaseInstance.getInstance().database.getExistingDocument(deletedUsernames.get(i));
                                    if(user!=null){
                                        if(((boolean)user.getProperties().get("deleted"))==false){
                                            Map<String,Object> userProps=new HashMap<>();
                                            userProps.putAll(user.getProperties());

                                            ArrayList<String> userAds=(ArrayList<String>)userProps.get("ads");
                                            for(int j=0;j<userAds.size();j++){
                                                Document ad=DatabaseInstance.getInstance().database.getExistingDocument(userAds.get(j));
                                                if(ad!=null){
                                                    if(((boolean)ad.getProperties().get("deleted"))==false){
                                                        Map<String,Object> adProps=new HashMap<>();
                                                        adProps.putAll(ad.getProperties());
                                                        adProps.put("deleted",true);
                                                        ad.putProperties(adProps);
                                                        Helper.resolveAdConflicts(ad.getId());
                                                    }
                                                }
                                            }

                                            userProps.put("deleted", true);
                                            user.putProperties(userProps);
                                            Helper.resolveUserConflicts(user.getId());
                                        }
                                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(HomeActivity.this);
                                        if(preferences.getString("Username", "").equals(deletedUsernames.get(i))){
                                            SharedPreferences.Editor editor = preferences.edit();
                                            editor.clear();
                                            editor.commit();
                                            Intent intent = new Intent(HomeActivity.this, HomeActivity.class);
                                            startActivity(intent);
                                        }
                                    }
                                }
                            } catch(CouchbaseLiteException e){
                                Log.e("Oglasi",e.getMessage());
                            }

                        }

                    }
                });

            } catch(Exception e){
                Log.e(TAG, e.getMessage());
            }

        }

        homeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long l) {
                String adId=((TextView) view.findViewById(R.id.ad_id)).getText().toString();
                if(Helper.isNetworkAvailable(getApplicationContext())){
                    Document adDoc = DatabaseInstance.getInstance().database.getExistingDocument(adId);
                    Map<String, Object> adProperties=new HashMap<String, Object>();
                    adProperties.putAll(adDoc.getProperties());

                    try {
                        URL url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                        pullUser = DatabaseInstance.getInstance().database.createPullReplication(url);
                        pullUser.setContinuous(false);
                        Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                        pullUser.setAuthenticator(auth);
                        List<String> docIds=new ArrayList<>();
                        docIds.add((String)adProperties.get("user_id"));
                        pullUser.setDocIds(docIds);
                        pullUser.start();

                        pullUser.addChangeListener(new Replication.ChangeListener() {
                            @Override
                            public void changed(Replication.ChangeEvent event) {
                                boolean active=deletedAdsPull.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE;
                                if(!active){
                                    userPulled=true;
                                }
                            }
                        });

                        while(!userPulled){
                            //wait...
                        }
                        userPulled=false;

                        Helper.resolveUserConflicts((String)adProperties.get("user_id"));

//                        while(DatabaseInstance.getInstance().database.getExistingDocument((String)adProperties.get("user_id"))==null){
//                            //wait...
//                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }

                Intent intent = new Intent(HomeActivity.this, AdActivity.class);
                intent.putExtra("AD_ID", adId);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(receiver,new IntentFilter("com.example.ivana.oglasi"));
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(receiver);
    }

    public String startAdIndex(){
        return ids.get(ids.size()-1);
    }

    public void ignoreDeletedAds(){
        Document deletedAdsDoc=DatabaseInstance.getInstance().database.getExistingDocument("ads_deleted");
        Map<String,Object> deletedProps=new HashMap<>();
        deletedProps.putAll(deletedAdsDoc.getProperties());
        deletedIds=(ArrayList<String>)deletedProps.get("ids");

        for(int i=0;i<deletedIds.size();i++){
            if(ids.contains(deletedIds.get(i)))
                ids.remove(deletedIds.get(i));
        }

        try {
            for(int i=0;i<deletedIds.size();i++){
                Document ad = DatabaseInstance.getInstance().database.getExistingDocument(deletedIds.get(i));
                if(ad != null){
                    Map<String,Object> adProps=new HashMap<>();
                    adProps.putAll(ad.getProperties());
                    adProps.put("deleted",true);
                    ad.putProperties(adProps);
                }
            }
        }
        catch (CouchbaseLiteException e){
            Log.e("Oglasi",e.getMessage());
        }

        new Thread(){
            @Override
            public void run(){
                for(int i=0;i<deletedIds.size();i++){
                    Document ad=DatabaseInstance.getInstance().database.getExistingDocument(deletedIds.get(i));
                    if(ad!=null){
                        if(dropbox.exists("/Oglasi/"+ad.getId())){
                            dropbox.delete("/Oglasi/"+ad.getId());
                        }
                    }
                }
            }
        }.start();
    }

    public void tryAdsPull(){
        if(Helper.isNetworkAvailable(getApplicationContext())){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog = ProgressDialog.show(HomeActivity.this, "Sačekajte...", "Sinhronizacija", false);
                }
            });

            List<String> adIds=new ArrayList<>();
            for(int i=ids.size()-1-adsLoadedCounter;i>=ids.size()-10-adsLoadedCounter;i--){
                    if(i<0)
                        break;
                    adIds.add(ids.get(i));
            }
            adsLoadedCounter+=adIds.size();
            pull.setDocIds(adIds);
            pull.start();
        }
        else{
            lastLoadedAdId=startAdIndex();
            homeListAdapter=new HomeListAdapter(getApplicationContext(),R.layout.home_list_item);
            this.loadAds();
            AlertDialog.Builder builder1 = new AlertDialog.Builder(HomeActivity.this);
            builder1.setMessage("Morate biti povezani na internet da biste osvežili prikaz oglasa.");
            builder1.setCancelable(true);

            builder1.setNeutralButton(
                    "Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
    }

    public void loadAds(){
        String last="-1";
        if(Helper.isNetworkAvailable(getApplicationContext())){
            while(!thumbnailsDownloaded){
                //wait for images
            }
        }

        thumbnailsDownloaded=false;
        for(int i=ids.size()-1;i>=ids.size()-adsLoadedCounter;i--){
            Document doc=DatabaseInstance.getInstance().database.getExistingDocument(ids.get(i));
            if(doc!=null && (boolean)doc.getProperties().get("deleted")==false){
                Map<String, Object> properties=new HashMap<String, Object>();
                properties.putAll(doc.getProperties());
                ArrayList<String> images=(ArrayList<String>)properties.get("images");
                Bitmap bm;
                if(images.size()!=0){
                    String[] folderAndName=images.get(0).split("/");
                    String path=folderAndName[0]+"/thumbnail_"+folderAndName[1];
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), path);
                    if(file.exists()){
                        Uri imgUri=Uri.fromFile(file);
                        bm= Helper.decodeSampledBitmapFromUri(imgUri,getApplicationContext(),200,200);
                    }
                    else{
                        bm= BitmapFactory.decodeResource(getResources(),R.drawable.no_image);
                    }
                }
                else{
                    bm= BitmapFactory.decodeResource(getResources(),R.drawable.no_image);
                }

                String adText=(String)properties.get("text");
                String upToNCharacters = adText.substring(0, Math.min(adText.length(), 50));
                upToNCharacters+="...";
                HomeListItemData homeListItemData=new HomeListItemData((String)properties.get("title"),upToNCharacters,bm,doc.getId());
                this.homeListAdapter.add(homeListItemData);
            }
            last=String.valueOf(i);
        }
        lastLoadedAdId=last;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                homeLoading.setVisibility(View.GONE);
                if(!footerAdded && adsLoadedCounter<ids.size()/*!lastLoadedAdId.equals("0")*/){
                    homeListView.addFooterView(mLoadMore);
                    footerAdded=true;
                }
                if(lastLoadedAdId.equals("0")){
                    homeListView.removeFooterView(mLoadMore);
                }
                if(loadedMore){
                    int index = homeListView.getFirstVisiblePosition();
                    View v = homeListView.getChildAt(0);
                    int top = (v == null) ? 0 : v.getTop();
                    homeListView.setAdapter(homeListAdapter);
                    homeListView.setSelectionFromTop(index, top);
                }
                else{
                    homeListView.setAdapter(homeListAdapter);
                }
            }
        });
    }

    public class ResolveAdCounterConflictsTask extends AsyncTask<Void,Void,Boolean> {
        protected Boolean doInBackground(Void... params) {
            try {
                url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                deletedAdsPull = DatabaseInstance.getInstance().database.createPullReplication(url);
                deletedAdsPull.setContinuous(false);
                Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                deletedAdsPull.setAuthenticator(auth);
                List<String> docId=new ArrayList<>();
                docId.add("ads_deleted");
                deletedAdsPull.setDocIds(docId);
                deletedAdsPull.start();

                deletedAdsPull.addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(Replication.ChangeEvent event) {
                        boolean active=deletedAdsPull.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE;
                        if(!active){
                            deletedAdsLoaded=true;
                        }
                    }
                });

                while(!deletedAdsLoaded){
                    //wait for result...
                }
                deletedAdsLoaded=false;

                Helper.resolveAdCounterConflicts();

            } catch (Exception e) {
                Log.e("Oglasi", e.getMessage());
                return false;
            }
            return true;
        }

        protected void onPostExecute(final Boolean success) {
            if(success){

                ignoreDeletedAds();

                if(initialAdsLoading){
                    initialAdsLoading=false;
                    adsLoadedCounter=0;

                    tryAdsPull();
                }
                else{
                    refresh=true;
                }
            }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(HomeActivity.this);
        if(preferences.getString("Username", "").isEmpty()){
            menu.findItem(R.id.action_logout).setVisible(false);
            menu.findItem(R.id.action_user).setVisible(false);
            menu.findItem(R.id.action_settings).setVisible(false);
            menu.findItem(R.id.action_add).setVisible(false);
            menu.findItem(R.id.action_login).setVisible(true);
        }
        else{
            menu.findItem(R.id.action_logout).setVisible(true);
            menu.findItem(R.id.action_user).setVisible(true);
            menu.findItem(R.id.action_settings).setVisible(true);
            menu.findItem(R.id.action_add).setVisible(true);
            menu.findItem(R.id.action_login).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_user) {
            String username=PreferenceManager.getDefaultSharedPreferences(HomeActivity.this).getString("Username", "");
            Document user=DatabaseInstance.getInstance().database.getExistingDocument(username);
            if(user==null){
                try {
                    URL url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                    final Replication pullUser = DatabaseInstance.getInstance().database.createPullReplication(url);
                    Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                    pullUser.setAuthenticator(auth);
                    List<String> docIds=new ArrayList<>();
                    docIds.add(username);
                    pullUser.setDocIds(docIds);
                    pullUser.start();

                    pullUser.addChangeListener(new Replication.ChangeListener() {
                        @Override
                        public void changed(Replication.ChangeEvent event) {
                            boolean active = (pullUser.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                            if (!active) {
                                userLoaded=true;
                            }
                        }
                    });
                    while(!userLoaded){
                        //wait...
                    }


                } catch (Exception e) {
                    Log.e("Oglasi", e.getMessage());
                }
            }
            Helper.resolveUserConflicts(username);
            Intent intent = new Intent(HomeActivity.this, UserActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_login) {
            Intent myIntent = new Intent(HomeActivity.this,LoginActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_logout){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent intent = new Intent(HomeActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(HomeActivity.this,"Uspešno ste se odjavili sa svog naloga.",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(id==R.id.action_add){
            Intent myIntent = new Intent(HomeActivity.this,NewAdActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_home){
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent myIntent = new Intent(HomeActivity.this,HomeActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }

}
