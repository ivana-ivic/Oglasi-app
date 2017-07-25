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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.support.LazyJsonArray;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {

    SearchView mSearch;
    Spinner mCat1;
    Spinner mCat2;
    ListView mAds;
    Button mFilter;
    HomeListAdapter adsListAdapter;
    String cat1="--";
    String cat2="--";
    ArrayList<Document> searchedDocs;
    boolean userLoaded=false;
    Replication pullAds;
    ProgressDialog pullProgress;
    ArrayList<String> ads;
    ArrayList<String> images;
    ProgressDialog downloadImagesProgress;
    CloudStorage dropbox;
    Replication pullUser;
    boolean catsApplied=false;
    ProgressDialog searchProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        invalidateOptionsMenu();

        CloudRail.setAppKey(DropboxCredentials.AppKey);
        dropbox = new Dropbox(getApplicationContext(), DropboxCredentials.API_ID, DropboxCredentials.API_KEY);

        if(Helper.isNetworkAvailable(getApplicationContext())){
            pullProgress=ProgressDialog.show(SearchActivity.this,"Sačekajte","Priprema podataka",true,false);
            try{
                URL url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                pullAds = DatabaseInstance.getInstance().database.createPullReplication(url);
                pullAds.setContinuous(false);
                Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                pullAds.setAuthenticator(auth);

                Document ads=DatabaseInstance.getInstance().database.getExistingDocument("ads_counter");
                Map<String,Object> adsProps=new HashMap<>();
                adsProps.putAll(ads.getProperties());
                ArrayList<String> allAds=(ArrayList<String>)adsProps.get("ids");

                Document deletedAdsDoc=DatabaseInstance.getInstance().database.getExistingDocument("ads_deleted");
                Map<String,Object> deletedAdsProps=new HashMap<>();
                deletedAdsProps.putAll(deletedAdsDoc.getProperties());
                ArrayList<String> deletedAds=(ArrayList<String>)deletedAdsProps.get("ids");

                for(int i=0;i<deletedAds.size();i++){
                    if(allAds.contains(deletedAds.get(i))){
                        allAds.remove(deletedAds.get(i));
                    }
                }

                List<String> docIds=new ArrayList<>();
                docIds.addAll(allAds);
                pullAds.setDocIds(docIds);
                if(Helper.isNetworkAvailable(getApplicationContext())){
                    pullAds.start();
                    pullAds.addChangeListener(new Replication.ChangeListener() {
                        @Override
                        public void changed(Replication.ChangeEvent event) {
                            boolean active = (pullAds.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                            if (!active) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pullProgress.dismiss();
                                    }
                                });
                            }
                        }
                    });
                }
                else{
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(SearchActivity.this);
                    builder1.setTitle("Pretraga oglasa");
                    builder1.setMessage("Možete da pretražujete samo oglase koje ste već učitali jer niste povezani na internet.");
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
            catch (Exception e){
                Log.e("Oglasi",e.getMessage());
            }
        }
        else{
            AlertDialog.Builder builder1 = new AlertDialog.Builder(SearchActivity.this);
            builder1.setTitle("Pretraga oglasa");
            builder1.setMessage("Možete da pretražujete samo oglase koje ste već učitali jer niste povezani na internet.");
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


        mSearch=(SearchView)findViewById(R.id.searchView_search);
        mAds=(ListView) findViewById(R.id.listView_searchAds);
        mFilter=(Button) findViewById(R.id.button_filteredSearch);

        adsListAdapter=new HomeListAdapter(getApplicationContext(),R.layout.home_list_item);
        searchedDocs=new ArrayList<>();

//        if(getIntent().getStringExtra("ad_cat1")!=null){
//            cat1=getIntent().getStringExtra("ad_cat1");
//            if(getIntent().getStringExtra("ad_cat2")!=null)
//                cat2=getIntent().getStringExtra("ad_cat2");
//        }

        mSearch.setQueryHint("Naslov oglasa...");
        mSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String newText) {

                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {

                Query queryData = DatabaseInstance.getInstance().adsView().createQuery();
                queryData.setMapOnly(true);
                queryData.setDescending(true);
                adsListAdapter=new HomeListAdapter(getApplicationContext(),R.layout.home_list_item);
                try{
                    QueryEnumerator result = queryData.run();
                    if(result.getCount()!=0){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                downloadImagesProgress=ProgressDialog.show(SearchActivity.this,"Pretraga","Sačekajte...",true,false);
                            }
                        });
                    }
                    ads=new ArrayList<String>();
                    images=new ArrayList<String>();
                    for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                        QueryRow row = it.next();
                        String title = (String) row.getValue();
                        if(title.toLowerCase().contains(query.toLowerCase())){
                            ads.add(row.getDocumentId());
                            Document ad=row.getDocument();
                            Map<String, Object> adProperties=new HashMap<String, Object>();
                            adProperties.putAll(ad.getProperties());
                            ArrayList<String> adImages=(ArrayList<String>)adProperties.get("images");
                            if(adImages.size()!=0){
                                String[] folderAndFileName=adImages.get(0).split("/");
                                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderAndFileName[0]+"/thumbnail_"+folderAndFileName[1]);
                                if(!file.exists()){
                                    images.add(adImages.get(0));
                                }
                            }
                        }
                    }
                }catch(CouchbaseLiteException e){
                    Log.e("Oglasi",e.getMessage());
                }

                if(images.size()!=0){
                    new Thread(){
                        @Override
                        public void run(){
                            if(Helper.isNetworkAvailable(getApplicationContext())){
                                for(int i=0;i<images.size();i++){
                                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), images.get(i));
                                    if(!file.exists()){
                                        if(dropbox.exists("/Oglasi/"+images.get(i))){
                                            InputStream result=dropbox.getThumbnail("/Oglasi/"+images.get(i));
                                            Bitmap bm = BitmapFactory.decodeStream(result);
                                            String[] folderAndFileName=images.get(i).split("/");
                                            new ImageSaver(getApplicationContext()).setExternal(true).setDirectoryName(folderAndFileName[0]).setFileName("thumbnail_"+folderAndFileName[1]).save(bm);
                                        }
                                    }
                                }
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    for(int i=0;i<ads.size();i++){
                                        Document doc=DatabaseInstance.getInstance().database.getExistingDocument(ads.get(i));
                                        if(doc!=null){
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
                                            adsListAdapter.add(homeListItemData);
                                            searchedDocs.add(doc);
                                        }
                                    }
                                    mAds.setAdapter(adsListAdapter);
                                    downloadImagesProgress.dismiss();
                                }
                            });
                        }
                    }.start();
                }
                else{
                    for(int i=0;i<ads.size();i++){
                        Document doc=DatabaseInstance.getInstance().database.getExistingDocument(ads.get(i));
                        if(doc!=null){
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
                            adsListAdapter.add(homeListItemData);
                            searchedDocs.add(doc);
                        }
                    }
                    mAds.setAdapter(adsListAdapter);
                    downloadImagesProgress.dismiss();
                }

                return true;
            }

        });

        mFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(SearchActivity.this,FiltersPopupActivity.class);
                intent.putExtra("cat1",cat1);
                intent.putExtra("cat2",cat2);
                startActivity(intent);
            }
        });

        mAds.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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

                        while(DatabaseInstance.getInstance().database.getExistingDocument((String)adProperties.get("user_id"))==null){
                            //wait...
                        }
                    } catch (Exception e) {
                        Log.e("Oglasi", e.getMessage());
                    }
                }

                Intent intent = new Intent(SearchActivity.this, AdActivity.class);
                intent.putExtra("AD_ID", adId);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent!=null)
            setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!catsApplied){
            catsApplied=!catsApplied;
            Intent intent=getIntent();
            if(intent.getStringExtra("cat1")!=null)
                this.cat1=getIntent().getExtras().getString("cat1");
            if(intent.getStringExtra("cat2")!=null)
                this.cat2=getIntent().getExtras().getString("cat2");

            if(!cat1.equals("--")){
                searchProgress=ProgressDialog.show(SearchActivity.this,"Pretraga","Sačekajte...",true,false);
                Query queryData = DatabaseInstance.getInstance().adsViewFilters().createQuery();
                queryData.setDescending(true);
                queryData.setMapOnly(true);
                try{
                    QueryEnumerator result = queryData.run();
                    adsListAdapter=new HomeListAdapter(getApplicationContext(),R.layout.home_list_item);
                    ads=new ArrayList<>();
                    images=new ArrayList<>();
                    for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                        QueryRow row = it.next();
                        LazyJsonArray<String> filters = (LazyJsonArray<String>) row.getValue();
                        if(filters.get(0).equals(cat1)){
                            if(cat2.equals("--")){
                                Document ad=row.getDocument();
                                Map<String, Object> adProperties=new HashMap<String, Object>();
                                adProperties.putAll(ad.getProperties());
                                ArrayList<String> adImages=(ArrayList<String>)adProperties.get("images");
                                if(adImages.size()!=0){
                                    String[] folderAndFileName=adImages.get(0).split("/");
                                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderAndFileName[0]+"/thumbnail_"+folderAndFileName[1]);
                                    if(!file.exists()){
                                        images.add(adImages.get(0));
                                    }
                                }
                                ads.add(ad.getId());
                            }
                            else{
                                if(filters.get(1).equals(cat2)){
                                    Document ad=row.getDocument();
                                    Map<String, Object> adProperties=new HashMap<String, Object>();
                                    adProperties.putAll(ad.getProperties());
                                    ArrayList<String> adImages=(ArrayList<String>)adProperties.get("images");
                                    if(adImages.size()!=0){
                                        String[] folderAndFileName=adImages.get(0).split("/");
                                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderAndFileName[0]+"/thumbnail_"+folderAndFileName[1]);
                                        if(!file.exists()){
                                            images.add(adImages.get(0));
                                        }
                                    }
                                    ads.add(ad.getId());
                                }
                            }
                        }
                    }

                    if(images.size()!=0){
                        new Thread(){
                            @Override
                            public void run(){
                                if(Helper.isNetworkAvailable(getApplicationContext())){
                                    for(int i=0;i<images.size();i++){
                                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), images.get(i));
                                        if(!file.exists()){
                                            if(dropbox.exists("/Oglasi/"+images.get(i))){
                                                InputStream result=dropbox.getThumbnail("/Oglasi/"+images.get(i));
                                                Bitmap bm = BitmapFactory.decodeStream(result);
                                                String[] folderAndFileName=images.get(i).split("/");
                                                new ImageSaver(getApplicationContext()).setExternal(true).setDirectoryName(folderAndFileName[0]).setFileName("thumbnail_"+folderAndFileName[1]).save(bm);
                                            }
                                        }
                                    }
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        for(int i=0;i<ads.size();i++){
                                            Document doc=DatabaseInstance.getInstance().database.getExistingDocument(ads.get(i));
                                            if(doc!=null){
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
                                                adsListAdapter.add(homeListItemData);
                                                searchedDocs.add(doc);
                                            }
                                        }
                                        mAds.setAdapter(adsListAdapter);
                                        searchProgress.dismiss();
                                    }
                                });
                            }
                        }.start();
                    }
                    else{
                        for(int i=0;i<ads.size();i++){
                            Document doc=DatabaseInstance.getInstance().database.getExistingDocument(ads.get(i));
                            if(doc!=null){
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
                                adsListAdapter.add(homeListItemData);
                                searchedDocs.add(doc);
                            }
                        }
                        mAds.setAdapter(adsListAdapter);
                        searchProgress.dismiss();
                    }

                }catch(CouchbaseLiteException e){

                }
                mAds.setAdapter(adsListAdapter);
            }
        }
     }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SearchActivity.this);
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
            Intent intent = new Intent(SearchActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_user) {
            String username=PreferenceManager.getDefaultSharedPreferences(SearchActivity.this).getString("Username", "");
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
                    Log.e("Oglasi", "Error occurred", e);
                }
            }
            Helper.resolveUserConflicts(username);
            Intent intent = new Intent(SearchActivity.this, UserActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(SearchActivity.this, SearchActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_login) {
            Intent myIntent = new Intent(SearchActivity.this,LoginActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_logout){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent intent = new Intent(SearchActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(SearchActivity.this,"Uspešno ste se odjavili sa svog naloga.",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(id==R.id.action_add){
            Intent myIntent = new Intent(SearchActivity.this,NewAdActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_home){
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent myIntent = new Intent(SearchActivity.this,HomeActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
