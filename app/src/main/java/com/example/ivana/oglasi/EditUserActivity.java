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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

public class EditUserActivity extends AppCompatActivity {

    EditText mEditEmail;
    EditText mEditPhone;
    Spinner mEditDistrict;
    Spinner mEditCity;
    EditText mEditDescription;
    Button mEditPassword;
    Button mSaveChanges;
    boolean userLoaded=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        invalidateOptionsMenu();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(EditUserActivity.this);
        String username = preferences.getString("Username", "");

        Document userDoc= DatabaseInstance.getInstance().database.getExistingDocument(username);
        Map<String, Object> userProperties=new HashMap<String, Object>();
        userProperties.putAll(userDoc.getProperties());

        mEditEmail=(EditText)findViewById(R.id.edit_email);
        mEditPhone=(EditText)findViewById(R.id.edit_phone);
        mEditDistrict=(Spinner)findViewById(R.id.spinner_editDistrict);
        mEditCity=(Spinner)findViewById(R.id.spinner_editCity);
        mEditDescription=(EditText)findViewById(R.id.edit_description);
        mEditPassword=(Button) findViewById(R.id.button_editPassword);
        mSaveChanges=(Button) findViewById(R.id.button_saveChanges);

        mEditEmail.setText(userProperties.get("email").toString());
        mEditPhone.setText(userProperties.get("phone").toString());
        mEditDescription.setText(userProperties.get("description").toString());

        ArrayAdapter<CharSequence> adapterDistrict = ArrayAdapter.createFromResource(this, R.array.district_items, android.R.layout.simple_spinner_item);
        adapterDistrict.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mEditDistrict.setAdapter(adapterDistrict);
        if (!userProperties.get("district").toString().equals(null)) {
            int spinnerPosition = adapterDistrict.getPosition(userProperties.get("district").toString());
            mEditDistrict.setSelection(spinnerPosition);
        }

        ArrayAdapter<CharSequence> adapterCity = ArrayAdapter.createFromResource(this, R.array.city_items, android.R.layout.simple_spinner_item);
        adapterCity.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mEditCity.setAdapter(adapterCity);
        if (!userProperties.get("city").toString().equals(null)) {
            int spinnerPosition = adapterCity.getPosition(userProperties.get("city").toString());
            mEditCity.setSelection(spinnerPosition);
        }

        mEditPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EditUserActivity.this, EditPasswordActivity.class);
                startActivity(intent);
            }
        });

        mSaveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(EditUserActivity.this);
                String username = preferences.getString("Username", "");
                Document user = DatabaseInstance.getInstance().database.getDocument(username);
                try {
                    Map<String, Object> updatedProperties = new HashMap<String, Object>();
                    updatedProperties.putAll(user.getProperties());
                    updatedProperties.put("email",mEditEmail.getText().toString());
                    updatedProperties.put("phone",mEditPhone.getText().toString());
                    updatedProperties.put("description",mEditDescription.getText().toString());
                    updatedProperties.put("district",mEditDistrict.getSelectedItem().toString());
                    updatedProperties.put("city",mEditCity.getSelectedItem().toString());
                    user.putProperties(updatedProperties);

                    Intent intent = new Intent(EditUserActivity.this, UserActivity.class);
                    startActivity(intent);
                } catch (CouchbaseLiteException e) {
                    Log.e("Oglasi",e.getMessage());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(EditUserActivity.this);
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
            Intent intent = new Intent(EditUserActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_user) {
            String username=PreferenceManager.getDefaultSharedPreferences(EditUserActivity.this).getString("Username", "");
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
            Intent intent = new Intent(EditUserActivity.this, UserActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(EditUserActivity.this, SearchActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_login) {
            Intent myIntent = new Intent(EditUserActivity.this,LoginActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_logout){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent intent = new Intent(EditUserActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(EditUserActivity.this,"Uspešno ste se odjavili sa svog naloga.",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(id==R.id.action_add){
            Intent myIntent = new Intent(EditUserActivity.this,NewAdActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_home){
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent myIntent = new Intent(EditUserActivity.this,HomeActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
