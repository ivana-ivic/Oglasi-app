package com.example.ivana.oglasi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.example.ivana.oglasi.Classes.DatabaseInstance;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DeletedAdsService extends Service {

    IBinder mBinder;
    Replication pullDeleted;

    public DeletedAdsService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        try {
            if(pullDeleted ==null){
                URL url = new URL(DatabaseInstance.getInstance().address + DatabaseInstance.getInstance().DB_NAME);
                pullDeleted = DatabaseInstance.getInstance().database.createPullReplication(url);
                pullDeleted.setContinuous(true);
                Authenticator auth = new BasicAuthenticator(DatabaseInstance.getInstance().databaseUsername, DatabaseInstance.getInstance().databasePassword);
                List<String> counterId=new ArrayList<>();
                counterId.add("ads_deleted");
                pullDeleted.setDocIds(counterId);
                pullDeleted.setAuthenticator(auth);
                pullDeleted.start();
            }
        } catch (Exception e) {
            Log.e("Oglasi", e.getMessage());
        }
        pullDeleted.addChangeListener(new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
                boolean active = (pullDeleted.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                if (!active) {
                    if(DatabaseInstance.getInstance().database.getDocumentCount()!=0){
                        Intent intent=new Intent("com.example.ivana.oglasi");
                        intent.putExtra("counterChanged", true);
                        sendBroadcast(intent);
                    }
                }
            }
        });
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
