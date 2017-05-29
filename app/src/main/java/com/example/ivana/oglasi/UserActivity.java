package com.example.ivana.oglasi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.Document;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.example.ivana.oglasi.Classes.DatabaseInstance;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserActivity extends AppCompatActivity {

    TextView mUsername;
    TextView mDescription;
    TextView mDistrict;
    TextView mCity;
    TextView mEmail;
    TextView mPhone;
    TextView mAdsLink;
    boolean userLoaded=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        invalidateOptionsMenu();

        String username = getIntent().getStringExtra("user_id");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(UserActivity.this);
        if(username==null){
            username = preferences.getString("Username", "");
            setTitle("Moj nalog");
        }

        Document userDoc= DatabaseInstance.getInstance().database.getExistingDocument(username);
        Map<String, Object> userProperties=new HashMap<String, Object>();
        userProperties.putAll(userDoc.getProperties());

        mUsername=(TextView)findViewById(R.id.textView_userUsername);
        mDescription=(TextView)findViewById(R.id.textView_userDescription);
        mDistrict=(TextView)findViewById(R.id.textView_userDistrict);
        mCity=(TextView)findViewById(R.id.textView_userCity);
        mEmail=(TextView)findViewById(R.id.textView_userEmail);
        mPhone=(TextView)findViewById(R.id.textView_userPhone);

        mUsername.setText((String)userProperties.get("_id"));
        mDescription.setText((String)userProperties.get("description"));
        mDistrict.setText((String)userProperties.get("district"));
        mCity.setText((String)userProperties.get("city"));
        mEmail.setText((String)userProperties.get("email"));
        mPhone.setText((String)userProperties.get("phone"));

        ArrayList ads=(ArrayList)userProperties.get("ads");
        String linkText;
        if(!preferences.getString("Username", "").isEmpty() && preferences.getString("Username", "").equals(mUsername.getText().toString()))
            linkText="Moji oglasi (" + String.valueOf(ads.size()) + ")";
        else
            linkText="Svi oglasi korisnika " + (String)userProperties.get("_id") + " (" + String.valueOf(ads.size()) + ")";
        mAdsLink=(TextView)findViewById(R.id.textView_userLinkToAds);
        mAdsLink.setText(linkText);

        mAdsLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserActivity.this,UserAdsActivity.class);
                intent.putExtra("user_id", mUsername.getText().toString());
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(UserActivity.this);
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
            Intent intent = new Intent(UserActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_user) {
            String username=PreferenceManager.getDefaultSharedPreferences(UserActivity.this).getString("Username", "");
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
            Intent intent = new Intent(UserActivity.this, UserActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(UserActivity.this, SearchActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_login) {
            Intent myIntent = new Intent(UserActivity.this,LoginActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_logout){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent intent = new Intent(UserActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(UserActivity.this,"Uspešno ste se odjavili sa svog naloga.",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(id==R.id.action_add){
            Intent myIntent = new Intent(UserActivity.this,NewAdActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_home){
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent myIntent = new Intent(UserActivity.this,HomeActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
