package com.example.ivana.oglasi;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.example.ivana.oglasi.Classes.DatabaseInstance;
import com.example.ivana.oglasi.Classes.Helper;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.example.ivana.oglasi.Classes.DatabaseInstance.DB_NAME;
import static com.example.ivana.oglasi.Classes.DatabaseInstance.address;

public class SettingsActivity extends AppCompatActivity {

    ImageButton mEditUser;
    ImageButton mDeleteUser;
    ImageButton mDeleteData;
    boolean userLoaded=false;
    ArrayList<String> imagesToDelete;
    boolean adsPulled=false;
    Replication pullAds;
    CloudStorage dropbox;
    ProgressDialog userDeletionProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        invalidateOptionsMenu();

        CloudRail.setAppKey("5911ab1dff21b5017c86daaa");
        dropbox = new Dropbox(getApplicationContext(), "ygrvukjpli1fs6p", "qd6c11dan34caef");

        mEditUser =(ImageButton)findViewById(R.id.imageButton_editUser);
        mDeleteUser =(ImageButton)findViewById(R.id.imageButton_deleteUser);
        mDeleteData =(ImageButton)findViewById(R.id.imageButton_deleteDatabase);

        mEditUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent edit=new Intent(SettingsActivity.this,EditUserActivity.class);
                startActivity(edit);
            }
        });

        mDeleteUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Helper.isNetworkAvailable(getApplicationContext())){
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(SettingsActivity.this);
                    builder1.setTitle("Brisanje naloga");
                    builder1.setMessage("Da li ste sigurni da želite da obrišete svoj nalog?");
                    builder1.setCancelable(true);

                    builder1.setPositiveButton("Da", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            userDeletionProgress=ProgressDialog.show(SettingsActivity.this,"Sačekajte","Brisanje naloga i oglasa...",true,false);
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
                            String username = preferences.getString("Username", "");
                            Document user= DatabaseInstance.getInstance().database.getDocument(username);
                            Map<String,Object> userProps=new HashMap<String, Object>();
                            userProps.putAll(user.getProperties());

                            ArrayList<String> userAds=(ArrayList<String>)userProps.get("ads");

                            try{
                                URL url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                                pullAds = DatabaseInstance.getInstance().database.createPullReplication(url);
                                pullAds.setContinuous(false);
                                Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                                pullAds.setAuthenticator(auth);
                                List<String> docIds=new ArrayList<>();
                                docIds.addAll(userAds);
                                pullAds.setDocIds(docIds);
                                pullAds.start();

                                pullAds.addChangeListener(new Replication.ChangeListener() {
                                    @Override
                                    public void changed(Replication.ChangeEvent event) {
                                        boolean active = (pullAds.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                                        if (!active) {
                                            adsPulled=true;
                                        }
                                    }
                                });
                            }
                            catch(Exception e){
                                Log.e("Oglasi",e.getMessage());
                            }

                            while(!adsPulled){
                                //wait...
                            }

                            imagesToDelete=new ArrayList<String>();
                            for(int i=0;i<userAds.size();i++){
                                Document ad=DatabaseInstance.getInstance().database.getExistingDocument(userAds.get(i));
                                Map<String,Object> adProps=new HashMap<String, Object>();
                                adProps.putAll(ad.getProperties());

                                ArrayList<String> adImages=(ArrayList<String>)adProps.get("images");

                                for(int j=0;j<adImages.size();j++){
                                    boolean deleted=false;
                                    String[] folderAndFileName=adImages.get(j).split("/");
                                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderAndFileName[0]);
                                    if(dir.exists()){
                                        deleted=Helper.deleteRecursive(dir);
                                    }
                                    imagesToDelete.add(adImages.get(j));
                                }

                                adProps.put("_deleted",true);

                                try{
                                    ad.putProperties(adProps);
                                }
                                catch(CouchbaseLiteException e){
                                    Log.e("Oglasi",e.getMessage());
                                }
                            }

                            new Thread(){
                                @Override
                                public void run() {
                                    for (int i = 0; i < imagesToDelete.size(); i++) {
                                        String[] folderAndFileName = imagesToDelete.get(i).split("/");
                                        if (dropbox.exists("/Oglasi/" + folderAndFileName[0])) {
                                            dropbox.delete("/Oglasi/" + folderAndFileName[0]);
                                        }
                                    }

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            userDeletionProgress.dismiss();
                                            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
                                            Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
                                            startActivity(intent);
                                        }
                                    });
                                }
                            }.start();

                            userProps.put("_deleted",true);

                            try{
                                user.putProperties(userProps);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.clear();
                                editor.commit();
                                Toast.makeText(SettingsActivity.this, "Uspešno ste obrisali svoj nalog.",Toast.LENGTH_LONG).show();
                            } catch(Exception e){
                                Log.e("Oglasi",e.getMessage());
                            }
                        }
                    });

                    builder1.setNegativeButton(
                            "Ne",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                }
                else{
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(SettingsActivity.this);
                    builder1.setMessage("Ne možete obrisati svoj nalog ukoliko niste povezani na internet.");
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

        mDeleteData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(SettingsActivity.this);
                builder1.setTitle("Brisanje podataka");
                builder1.setMessage("Ova opcija će obrisati sve podatke iz telefona. Da li želite da nastavite dalje?");
                builder1.setCancelable(true);

                builder1.setPositiveButton(
                        "Da",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Query queryData = DatabaseInstance.getInstance().adsView().createQuery();
                                queryData.setMapOnly(true);
                                try{
                                    QueryEnumerator result = queryData.run();
                                    for(Iterator<QueryRow> it = result; it.hasNext();){
                                        QueryRow row = it.next();
                                        String adId=row.getDocumentId();
                                        boolean deleted=false;
                                        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), adId);
                                        if(dir.exists()){
                                            deleted=Helper.deleteRecursive(dir);
                                        }
                                    }
                                }
                                catch (CouchbaseLiteException e){
                                    Log.e("Oglasi",e.getMessage());
                                }

                                DatabaseInstance.getInstance().deleteDatabase();
                                stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
                                Intent adIntent=new Intent(SettingsActivity.this,HomeActivity.class);
                                adIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                finish();
                                startActivity(adIntent);
                            }
                        });

                builder1.setNegativeButton(
                        "Ne",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert11 = builder1.create();
                alert11.show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
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
            Intent intent = new Intent(SettingsActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_user) {
            String username=PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).getString("Username", "");
            Document user=DatabaseInstance.getInstance().database.getExistingDocument(username);
            if(user==null){
                try {
                    URL url = new URL(address + DB_NAME);
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
            Intent intent = new Intent(SettingsActivity.this, UserActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(SettingsActivity.this, SearchActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_login) {
            Intent myIntent = new Intent(SettingsActivity.this,LoginActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_logout){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(SettingsActivity.this,"Uspešno ste se odjavili sa svog naloga.",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(id==R.id.action_add){
            Intent myIntent = new Intent(SettingsActivity.this,NewAdActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_home){
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent myIntent = new Intent(SettingsActivity.this,HomeActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
