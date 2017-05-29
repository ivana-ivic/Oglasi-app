package com.example.ivana.oglasi;

import android.content.Intent;
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
import com.couchbase.lite.util.Log;
import com.example.ivana.oglasi.Classes.DatabaseInstance;

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
                    Document user=DatabaseInstance.getInstance().database.getExistingDocument(mUsername.getText().toString());
                    if(user!=null){
                        mUsername.setError("Korisnički nalog sa ovim imenom već postoji");
                        Map<String, Object> properties=new HashMap<String, Object>();
                        properties.putAll(user.getProperties());
                        if(properties.get("email").equals(mEmail.getText().toString())){
                            mEmail.setError("Korisnički nalog sa ovom email adresom već postoji");
                        }
                        success=false;
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
                    try {
                        document.putProperties(map);
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
