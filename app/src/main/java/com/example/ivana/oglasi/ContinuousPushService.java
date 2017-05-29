package com.example.ivana.oglasi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.example.ivana.oglasi.Classes.DatabaseInstance;

import java.net.URL;

public class ContinuousPushService extends Service {

    IBinder mBinder;
    boolean mAllowRebind;
    Replication continuousPush;

    public ContinuousPushService() {
    }

    @Override
    public void onCreate(){
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        try {
            if(continuousPush==null){
                URL url = new URL(DatabaseInstance.getInstance().address + DatabaseInstance.getInstance().DB_NAME);
                continuousPush = DatabaseInstance.getInstance().database.createPushReplication(url);
                continuousPush.setContinuous(true);
                Authenticator auth = new BasicAuthenticator(DatabaseInstance.getInstance().databaseUsername, DatabaseInstance.getInstance().databasePassword);
                continuousPush.setAuthenticator(auth);
                continuousPush.start();
            }
        } catch (Exception e) {
            Log.e("Oglasi", e.getMessage());
        }
        continuousPush.addChangeListener(new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
                boolean active = (continuousPush.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                if (!active) {
                    Log.i("Oglasi","Push completed");
                }
            }
        });
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent){

    }

    @Override
    public void onDestroy(){
        Toast.makeText(getApplicationContext(),"Service destroyed",Toast.LENGTH_LONG).show();
    }
}
