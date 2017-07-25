package com.example.ivana.oglasi;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
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

public class EditPasswordActivity extends AppCompatActivity {

    EditText mOldPassword;
    EditText mNewPassword;
    EditText mRepeatNewPassword;
    Button mConfirm;
    boolean userLoaded=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_password);

        invalidateOptionsMenu();

        mOldPassword=(EditText)findViewById(R.id.editText_oldPassword);
        mNewPassword=(EditText)findViewById(R.id.editText_newPassword);
        mRepeatNewPassword=(EditText)findViewById(R.id.editText_repeatNewPassword);
        mConfirm=(Button)findViewById(R.id.button_confirmChangePassword);

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean success=true;
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(EditPasswordActivity.this);
                String username = preferences.getString("Username", "");
                Document userDoc= DatabaseInstance.getInstance().database.getExistingDocument(username);
                Map<String, Object> userProperties=new HashMap<String, Object>();
                userProperties.putAll(userDoc.getProperties());

                if(mOldPassword.getText().toString().trim().equalsIgnoreCase("")){
                    mOldPassword.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mOldPassword.setError(null);
                    if(!userProperties.get("password").toString().equals(mOldPassword.getText().toString())){
                        mOldPassword.setError("Neispravna stara lozinka");
                        success=false;
                    }
                }

                if(mRepeatNewPassword.getText().toString().trim().equalsIgnoreCase("")){
                    mRepeatNewPassword.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mRepeatNewPassword.setError(null);
                }

                if(mNewPassword.getText().toString().trim().equalsIgnoreCase("")){
                    mNewPassword.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mNewPassword.setError(null);
                    if(!mNewPassword.getText().toString().trim().equalsIgnoreCase("") && !mRepeatNewPassword.getText().toString().equals(mNewPassword.getText().toString())){
                        mRepeatNewPassword.setError("Lozinka i ponovljena lozinka se ne slažu");
                        success=false;
                    }
                }

                if(success){
                    try{
                        userProperties.put("password",mNewPassword.getText().toString());
                        userDoc.putProperties(userProperties);
//                        DatabaseInstance.getInstance().tryPush();
                        Intent intent = new Intent(EditPasswordActivity.this, EditUserActivity.class);
                        startActivity(intent);
                    } catch (CouchbaseLiteException e) {
                        return;
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(EditPasswordActivity.this);
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
            Intent intent = new Intent(EditPasswordActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_user) {
            String username=PreferenceManager.getDefaultSharedPreferences(EditPasswordActivity.this).getString("Username", "");
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
            Intent intent = new Intent(EditPasswordActivity.this, UserActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(EditPasswordActivity.this, SearchActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_login) {
            Intent myIntent = new Intent(EditPasswordActivity.this,LoginActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_logout){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent intent = new Intent(EditPasswordActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(EditPasswordActivity.this,"Uspešno ste se odjavili sa svog naloga.",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(id==R.id.action_add){
            Intent myIntent = new Intent(EditPasswordActivity.this,NewAdActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_home){
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent myIntent = new Intent(EditPasswordActivity.this,HomeActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
