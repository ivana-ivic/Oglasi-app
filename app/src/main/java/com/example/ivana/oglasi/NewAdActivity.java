package com.example.ivana.oglasi;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.TransactionalTask;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.example.ivana.oglasi.Classes.DatabaseInstance;
import com.example.ivana.oglasi.Classes.Helper;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewAdActivity extends AppCompatActivity {

    EditText mNewAdTitle;
    EditText mNewAdText;
    Spinner mNewAdCat1;
    Spinner mNewAdCat2;
    Button mAddNewAd;
    static final int[] LOOKUP_TABLE=new int[]{
            R.array.__,
            R.array.aksesoari,
            R.array.muska_odeca,
            R.array.zenska_odeca,
            R.array.kucni_ljubimci,
            R.array.kuce,
            R.array.stanovi,
            R.array.tehnika,
            R.array.ostalo
    };
    boolean userLoaded=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_ad);

        invalidateOptionsMenu();

        mNewAdTitle=(EditText)findViewById(R.id.editText_newAdTitle);
        mNewAdText=(EditText)findViewById(R.id.editText_newAdText);
        mNewAdCat1=(Spinner)findViewById(R.id.spinner_newAdCat1);
        mNewAdCat2=(Spinner)findViewById(R.id.spinner_newAdCat2);
        mAddNewAd=(Button) findViewById(R.id.button_addNewAd);

        String[] cat1Data=getResources().getStringArray(R.array.cat_1);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(NewAdActivity.this,android.R.layout.simple_spinner_dropdown_item,cat1Data);
        mNewAdCat1.setAdapter(adapter);

        mNewAdCat1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] cat2Data=getResources().getStringArray(LOOKUP_TABLE[position]);
                ArrayAdapter<String> adapter=new ArrayAdapter<String>(NewAdActivity.this,android.R.layout.simple_spinner_dropdown_item,cat2Data);
                mNewAdCat2.setAdapter(adapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mAddNewAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean success=true;

                if(mNewAdTitle.getText().toString().trim().equalsIgnoreCase("")){
                    mNewAdTitle.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mNewAdTitle.setError(null);
                }

                if(mNewAdText.getText().toString().trim().equalsIgnoreCase("")){
                    mNewAdText.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mNewAdText.setError(null);
                }

                if(mNewAdCat1.getSelectedItem().toString().equals("--")){
                    Toast.makeText(getApplicationContext(), "Morate izabrati kategoriju", Toast.LENGTH_LONG).show();
                    success=false;
                }

                if(mNewAdCat2.getSelectedItem().toString().equals("--")){
                    Toast.makeText(getApplicationContext(), "Morate izabrati potkategoriju", Toast.LENGTH_LONG).show();
                    success=false;
                }

                if(success){
                    Document adsCounter=DatabaseInstance.getInstance().database.getDocument("ads_counter");
                    Map<String, Object> mapCounter = new HashMap<String, Object>();
                    mapCounter.putAll(adsCounter.getProperties());
                    ArrayList<String> counter=(ArrayList<String>)mapCounter.get("ids");
                    Document newAd = DatabaseInstance.getInstance().database.createDocument();
                    counter.add(newAd.getId());
                    mapCounter.put("ids",counter);
                    Map<String, Object> mapAd = new HashMap<String, Object>();
                    mapAd.put("title",mNewAdTitle.getText().toString());
                    mapAd.put("text",mNewAdText.getText().toString());
                    mapAd.put("created_at",System.currentTimeMillis() / 1000L);
                    mapAd.put("updated_at",System.currentTimeMillis() / 1000L);
                    mapAd.put("comments",new ArrayList<>());
                    mapAd.put("images",new ArrayList<>());
                    mapAd.put("report_flag",false);
                    mapAd.put("report_count",0);
                    mapAd.put("_deleted",false);

                    ArrayList<String> filters=new ArrayList<String>();
                    filters.add(mNewAdCat1.getSelectedItem().toString());
                    filters.add(mNewAdCat2.getSelectedItem().toString());
                    mapAd.put("filters",filters);
                    mapAd.put("cat1",mNewAdCat1.getSelectedItem().toString());
                    mapAd.put("cat2",mNewAdCat2.getSelectedItem().toString());

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(NewAdActivity.this);
                    String username=preferences.getString("Username", "");
                    mapAd.put("user_id",username);
                    Document userDoc=DatabaseInstance.getInstance().database.getExistingDocument(username);

                    if(userDoc==null){
                        try {
                            URL url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                            Replication pullUser = DatabaseInstance.getInstance().database.createPullReplication(url);
                            pullUser.setContinuous(false);
                            Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                            pullUser.setAuthenticator(auth);
                            List<String> docIds=new ArrayList<>();
                            docIds.add(username);
                            pullUser.setDocIds(docIds);
                            pullUser.start();

                            while(DatabaseInstance.getInstance().database.getExistingDocument(username)==null){
                                //wait...
                            }
                            userDoc=DatabaseInstance.getInstance().database.getExistingDocument(username);
                        } catch (Exception e) {
                            Log.e("NewAdActivity",e.getMessage());
                        }
                    }

                    Map<String, Object> mapUser = new HashMap<String, Object>();
                    mapUser.putAll(userDoc.getProperties());
                    ArrayList<String> userAds=(ArrayList<String>) mapUser.get("ads");
                    userAds.add(counter.get(counter.size()-1));
                    mapUser.put("ads",userAds);

                    try {
                        adsCounter.putProperties(mapCounter);
                        newAd.putProperties(mapAd);
                        userDoc.putProperties(mapUser);
                    } catch (CouchbaseLiteException e) {
                        com.couchbase.lite.util.Log.e("NewAdActivity",e.getMessage());
                    }

                    new Thread(){
                        @Override
                        public void run(){
                            DatabaseInstance.getInstance().tryPull("ads_counter");
                            Helper.resolveAdCounterConflicts();
                        }
                    }.start();

                    Intent intent=new Intent(NewAdActivity.this,AddImagesActivity.class);
                    intent.putExtra("ad_id",counter.get(counter.size()-1));
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(NewAdActivity.this);
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
            Intent intent = new Intent(NewAdActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_user) {
            String username=PreferenceManager.getDefaultSharedPreferences(NewAdActivity.this).getString("Username", "");
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
            Intent intent = new Intent(NewAdActivity.this, UserActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(NewAdActivity.this, SearchActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_login) {
            Intent myIntent = new Intent(NewAdActivity.this,LoginActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_logout){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent intent = new Intent(NewAdActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(NewAdActivity.this,"Uspešno ste se odjavili sa svog naloga.",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(id==R.id.action_add){
            Intent myIntent = new Intent(NewAdActivity.this,NewAdActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_home){
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent myIntent = new Intent(NewAdActivity.this,HomeActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
