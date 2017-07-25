package com.example.ivana.oglasi;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.couchbase.lite.Attachment;
import com.couchbase.lite.Document;
import com.couchbase.lite.Revision;
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
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdsActivity extends AppCompatActivity {

    HomeListAdapter adsListAdapter;
    ListView adsListView;
    ArrayList<String> userAds;
    boolean userLoaded=false;
    boolean adsLoaded=false;
    boolean thumbnailsLoaded=false;
    CloudStorage dropbox;
    ProgressBar adsLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_ads);

        invalidateOptionsMenu();

        CloudRail.setAppKey(DropboxCredentials.AppKey);
        dropbox = new Dropbox(getApplicationContext(), DropboxCredentials.API_ID, DropboxCredentials.API_KEY);

        adsListAdapter=new HomeListAdapter(getApplicationContext(),R.layout.home_list_item);
        adsListView=(ListView)findViewById(R.id.user_ads);
        adsLoading=(ProgressBar)findViewById(R.id.progressBar_adsLoading);

        String username = getIntent().getStringExtra("user_id");
        if(username==null){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(UserAdsActivity.this);
            username = preferences.getString("Username", "");
        }
        Document userDoc= DatabaseInstance.getInstance().database.getExistingDocument(username);
        Map<String, Object> userProperties=new HashMap<String, Object>();
        userProperties.putAll(userDoc.getProperties());

        userAds=(ArrayList<String>) userProperties.get("ads");

        adsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long l) {
                String adId = userAds.get(position);
                Intent intent = new Intent(UserAdsActivity.this, AdActivity.class);
                intent.putExtra("AD_ID", adId);
                startActivity(intent);
            }
        });

        if(Helper.isNetworkAvailable(getApplicationContext())){
            try {
                URL url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                final Replication pullAds = DatabaseInstance.getInstance().database.createPullReplication(url);
                pullAds.setContinuous(false);
                Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                pullAds.setAuthenticator(auth);
                pullAds.setDocIds(userAds);
                pullAds.start();

                pullAds.addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(Replication.ChangeEvent event) {
                        boolean active = (pullAds.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                        if (!active) {
                            loadAdsOnline();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("Oglasi", e.getMessage());
            }
        }
        else{
            AlertDialog.Builder builder1 = new AlertDialog.Builder(UserAdsActivity.this);
            builder1.setMessage("Morate biti povezani na internet da biste učitali sve oglase korisnika.");
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

            loadAdsOffline();
        }
    }

    public void loadAdsOnline(){

        new Thread(){
            @Override
            public void run(){
                for(int i=0;i<userAds.size();i++){
                    try{
                        Document ad=DatabaseInstance.getInstance().database.getExistingDocument(userAds.get(i));
                        if(ad!=null){
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
                    catch(Exception e){
                        Log.e("Oglasi",e.getMessage());
                    }
                }
                thumbnailsLoaded=true;
            }
        }.start();

        while(!thumbnailsLoaded){
            //wait...
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (userAds.size() != 0) {
                    findViewById(R.id.textView_noAds).setVisibility(View.GONE);
                }

                for(int i=0;i<userAds.size();i++){
                    Document doc=DatabaseInstance.getInstance().database.getExistingDocument(userAds.get(i));
                    if(doc!=null){
                        if((boolean)doc.getProperties().get("deleted")==false){
                            Map<String, Object> properties=new HashMap<String, Object>();
                            properties.putAll(doc.getProperties());
                            Bitmap bm;
                            ArrayList<String> adImages=(ArrayList<String>)properties.get("images");
                            if(adImages.size()!=0){
                                String[] folderAndName=adImages.get(0).split("/");
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
                            try{
                                String adText=(String)properties.get("text");
                                String upToNCharacters = adText.substring(0, Math.min(adText.length(), 50));
                                upToNCharacters+="...";
                                HomeListItemData homeListItemData=new HomeListItemData((String)properties.get("title"),upToNCharacters,bm,doc.getId());
                                adsListAdapter.add(homeListItemData);
                            }
                            catch(Exception e){
                                Log.e("Oglasi",e.getMessage());
                            }
                        }
                    }
                }
                adsLoading.setVisibility(View.GONE);
                adsListView.setAdapter(adsListAdapter);
            }
        });
    }

    public void loadAdsOffline(){
        ArrayList<String> docsLoaded=new ArrayList<>();
        for(int i=0;i<userAds.size();i++){
            if(DatabaseInstance.getInstance().database.getExistingDocument(userAds.get(i))!=null)
                docsLoaded.add(userAds.get(i));
        }

        if(docsLoaded.size()!=0)
            findViewById(R.id.textView_noAds).setVisibility(View.GONE);

        for(int i=0;i<docsLoaded.size();i++){
            Document doc=DatabaseInstance.getInstance().database.getDocument(docsLoaded.get(i));
            Map<String, Object> properties=new HashMap<String, Object>();
            properties.putAll(doc.getProperties());
            ArrayList<String> adImages=(ArrayList<String>)properties.get("images");
            Bitmap bm;
            try{
                if(adImages.size()!=0){
                    String[] folderAndName=adImages.get(0).split("/");
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
                this.adsListAdapter.add(homeListItemData);
            }
            catch(Exception e){
                Log.e("Oglasi",e.getMessage());
            }
        }
        adsListView.setAdapter(adsListAdapter);
        adsLoading.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(UserAdsActivity.this);
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
            Intent intent = new Intent(UserAdsActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_user) {
            String username=PreferenceManager.getDefaultSharedPreferences(UserAdsActivity.this).getString("Username", "");
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
            Intent intent = new Intent(UserAdsActivity.this, UserActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(UserAdsActivity.this, SearchActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_login) {
            Intent myIntent = new Intent(UserAdsActivity.this,LoginActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_logout){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent intent = new Intent(UserAdsActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(UserAdsActivity.this,"Uspešno ste se odjavili sa svog naloga.",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(id==R.id.action_add){
            Intent myIntent = new Intent(UserAdsActivity.this,NewAdActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_home){
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent myIntent = new Intent(UserAdsActivity.this,HomeActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
