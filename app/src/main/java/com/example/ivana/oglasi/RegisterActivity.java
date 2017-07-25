package com.example.ivana.oglasi;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
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

public class RegisterActivity extends AppCompatActivity {

    EditText mUsername;
    EditText mEmail;
    EditText mPhone;
    Spinner mDistrict;
    Spinner mCity;
    EditText mDescription;
    EditText mPassword;
    EditText mRepeatPassword;
    Button mRegister;
    boolean usernamesPulled=false;
    Replication pullUsernames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mUsername=(EditText)findViewById(R.id.editText_regUsername);
        mEmail=(EditText)findViewById(R.id.editText_regEmail);
        mPhone=(EditText)findViewById(R.id.editText_regPhone);
        mDistrict=(Spinner)findViewById(R.id.spinner_regDistrict);
        mCity=(Spinner)findViewById(R.id.spinner_regCity);
        mDescription=(EditText)findViewById(R.id.editText_regDescription);
        mPassword=(EditText)findViewById(R.id.editText_regPassword);
        mRepeatPassword =(EditText)findViewById(R.id.editText_regRepeatPassword);
        mRegister=(Button)findViewById(R.id.button_register);

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean success=true;

                if(mEmail.getText().toString().trim().equalsIgnoreCase("")){
                    mEmail.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mEmail.setError(null);
                    if(!TextUtils.isEmpty(mEmail.getText().toString()) && !Patterns.EMAIL_ADDRESS.matcher(mEmail.getText().toString()).matches()){
                        mEmail.setError("Email nije validnog formata");
                        success=false;
                    }
                }

                if(mUsername.getText().toString().trim().equalsIgnoreCase("")){
                    mUsername.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mUsername.setError(null);
                    if(Helper.isNetworkAvailable(getApplicationContext())){
                        try{
                            URL url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                            pullUsernames = DatabaseInstance.getInstance().database.createPullReplication(url);
                            pullUsernames.setContinuous(false);
                            Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                            pullUsernames.setAuthenticator(auth);
                            List<String> ids=new ArrayList<>();
                            ids.add("usernames");
                            pullUsernames.setDocIds(ids);
                            pullUsernames.start();

                            pullUsernames.addChangeListener(new Replication.ChangeListener() {
                                @Override
                                public void changed(Replication.ChangeEvent event) {
                                    boolean active = (pullUsernames.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                                    if (!active){
                                        usernamesPulled=true;
                                    }
                                }
                            });

                            while(!usernamesPulled){
                                //wait...
                            }


                        } catch(Exception e){
                            Log.e("Oglasi",e.getMessage());
                        }

                        Document usernamesDoc=DatabaseInstance.getInstance().database.getExistingDocument("usernames");
                        ArrayList<String> usernames=(ArrayList<String>)usernamesDoc.getProperties().get("ids");
                        if(usernames.contains(mUsername.getText().toString())){
                            mUsername.setError("Korisnički nalog sa ovim imenom već postoji");
                            success=false;
                        }
                        else{
                            mUsername.setError(null);
                        }
                    }
                    else{
                        success=false;
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(RegisterActivity.this);
                        builder1.setTitle("Registracija");
                        builder1.setMessage("Morate biti povezani na internet kako bi se proverilo da li je korisnički ime zauzeto.");
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

                if(mPhone.getText().toString().trim().equalsIgnoreCase("")){
                    mPhone.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mPhone.setError(null);
                }

                if(mPassword.getText().toString().trim().equalsIgnoreCase("")){
                    mPassword.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mPassword.setError(null);
                }

                if(mRepeatPassword.getText().toString().trim().equalsIgnoreCase("")){
                    mRepeatPassword.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mRepeatPassword.setError(null);
                    if(!mPassword.getText().toString().trim().equalsIgnoreCase("") && !mRepeatPassword.getText().toString().equals(mPassword.getText().toString())){
                        mRepeatPassword.setError("Lozinka i ponovljena lozinka se ne slažu");
                        success=false;
                    }
                }

                if(success){

                    Document usernamesDoc=DatabaseInstance.getInstance().database.getExistingDocument("usernames");
                    Map<String,Object> usernamesProps=new HashMap<>();
                    usernamesProps.putAll(usernamesDoc.getProperties());
                    ArrayList<String> usernames=(ArrayList<String>) usernamesProps.get("ids");
                    usernames.add(mUsername.getText().toString());
                    usernamesProps.put("ids", usernames);

                    Document document = DatabaseInstance.getInstance().database.getDocument(mUsername.getText().toString());
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("email", mEmail.getText().toString());
                    map.put("phone", mPhone.getText().toString());
                    map.put("password", mPassword.getText().toString());
                    map.put("district", mDistrict.getSelectedItem().toString());
                    map.put("city", mCity.getSelectedItem().toString());
                    map.put("description", mDescription.getText().toString());
                    ArrayList<String> ads=new ArrayList<String>();
                    map.put("ads",ads);
                    map.put("deleted",false);
                    try {
                        document.putProperties(map);
                        usernamesDoc.putProperties(usernamesProps);
                        Helper.resolveUsernamesListConflicts();
                    } catch (CouchbaseLiteException e) {
                        Log.e("Oglasi", e.getMessage());
                    }
                    Toast.makeText(RegisterActivity.this, "Registracija uspešna. Možete se prijaviti na svoj nalog.",Toast.LENGTH_LONG).show();
                    Intent login=new Intent(RegisterActivity.this,LoginActivity.class);
                    login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(login);
                }

                return;
            }
        });
    }
}
