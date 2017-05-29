package com.example.ivana.oglasi;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.couchbase.lite.Document;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.example.ivana.oglasi.Classes.DatabaseInstance;

import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dropbox.core.v2.sharing.ListFileMembersIndividualResult.Tag.RESULT;

public class AdCounterPullService extends Service {

    IBinder mBinder;
    Replication pullCounter;
    ArrayList<String> ids=new ArrayList<>();
    boolean initialBroadcast=true;

    public AdCounterPullService() {
        Document adsCounterDoc=DatabaseInstance.getInstance().database.getExistingDocument("ads_counter");
        if(adsCounterDoc!=null) {
            Map<String, Object> counterProps = new HashMap<String, Object>();
            counterProps.putAll(adsCounterDoc.getProperties());
            ids = (ArrayList<String>) counterProps.get("ids");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        try {
            URL url = new URL(DatabaseInstance.getInstance().address + DatabaseInstance.getInstance().DB_NAME);
            pullCounter = DatabaseInstance.getInstance().database.createPullReplication(url);
            pullCounter.setContinuous(true);
            Authenticator auth = new BasicAuthenticator(DatabaseInstance.getInstance().databaseUsername, DatabaseInstance.getInstance().databasePassword);
            List<String> counterId=new ArrayList<>();
            counterId.add("ads_counter");
            pullCounter.setDocIds(counterId);
            pullCounter.setAuthenticator(auth);
            pullCounter.start();
        } catch (Exception e) {
            Log.e("Oglasi", e.getMessage());
        }
        pullCounter.addChangeListener(new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
                boolean active = (pullCounter.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                if (!active) {
                    if(DatabaseInstance.getInstance().database.getDocumentCount()!=0){
                        Document adsCounterDoc=DatabaseInstance.getInstance().database.getExistingDocument("ads_counter");
                        if(adsCounterDoc!=null){
                            Map<String,Object> counterProps=new HashMap<String, Object>();
                            counterProps.putAll(adsCounterDoc.getProperties());
                            ArrayList<String> newIds=(ArrayList<String>)counterProps.get("ids");
                            if(initialBroadcast && newIds.size()==ids.size()){
                                initialBroadcast=false;
                                Intent intent=new Intent("com.example.ivana.oglasi");
                                intent.putExtra("counterChanged", true);
                                intent.putExtra("ids", ids);
                                sendBroadcast(intent);
                            }
                            if(newIds.size()>ids.size()){
                                ids.clear();
                                ids.addAll(newIds);
                                Intent intent=new Intent("com.example.ivana.oglasi");
                                intent.putExtra("counterChanged", true);
                                intent.putExtra("ids", ids);
                                sendBroadcast(intent);
                            }
                        }
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
