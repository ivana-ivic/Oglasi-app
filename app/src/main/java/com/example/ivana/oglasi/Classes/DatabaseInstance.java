package com.example.ivana.oglasi.Classes;

import android.content.Intent;

import com.couchbase.lite.Context;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.example.ivana.oglasi.ContinuousPushService;
import com.example.ivana.oglasi.HomeActivity;
import com.example.ivana.oglasi.R;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ivana on 1/24/2017.
 */

public class DatabaseInstance {

    public static final String address="http://192.168.0.103:5984/";
    public static final String DB_NAME = "test";
    public static final String databaseUsername = "Ivana";
    public static final String databasePassword = "ivanacouchadmin";
    public static final String TAG = "Oglasi";
    public Manager manager = null;
    public Database database = null;
    Replication push;
    Replication pull;
    com.couchbase.lite.View adsView;
    com.couchbase.lite.View adsViewFilters;
    private static DatabaseInstance databaseInstance=null;

    private DatabaseInstance(){
        try{
            this.manager = new Manager(HomeActivity.androidContext, Manager.DEFAULT_OPTIONS);
            this.database=this.manager.getDatabase(DB_NAME);
        } catch(Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    public static DatabaseInstance getInstance() {
        if(databaseInstance==null)
            databaseInstance=new DatabaseInstance();
        return databaseInstance;
    }

    public void pullUser(String userId){
        try {
            URL url = new URL(address + DB_NAME);
            Replication pullUser = DatabaseInstance.getInstance().database.createPullReplication(url);
            pullUser.setContinuous(false);
            Authenticator auth = new BasicAuthenticator(databaseUsername, databasePassword);
            pullUser.setAuthenticator(auth);
            List<String> docIds=new ArrayList<>();
            docIds.add(userId);
            pullUser.setDocIds(docIds);
            pullUser.start();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public boolean tryPush(String docId){
        try {
            URL url = new URL(address + DB_NAME);
            push = DatabaseInstance.getInstance().database.createPushReplication(url);
            push.setContinuous(false);
            Authenticator auth = new BasicAuthenticator(databaseUsername, databasePassword);
            push.setAuthenticator(auth);
            ArrayList<String> docIds=new ArrayList<String>();
            docIds.add(docId);
            push.setDocIds(docIds);
            push.start();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean tryPull(String docId){
        try {
            URL url = new URL(address + DB_NAME);
            pull = DatabaseInstance.getInstance().database.createPushReplication(url);
            pull.setContinuous(false);
            Authenticator auth = new BasicAuthenticator(databaseUsername, databasePassword);
            pull.setAuthenticator(auth);
            ArrayList<String> docIds=new ArrayList<String>();
            docIds.add(docId);
            pull.setDocIds(docIds);
            pull.start();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        return true;
    }

    public com.couchbase.lite.View cat1View(final String cat1) {
        com.couchbase.lite.View cat1View = this.database.getView(cat1);
        if (cat1View.getMap() == null) {
            cat1View.setMap(
                    new Mapper(){
                        @Override
                        public void map(Map<String, Object> document, Emitter emitter) {
                            String cat1Value = (String)document.get("cat1");
                            if(cat1Value.equals(cat1)){
                                emitter.emit(document.get("_id"),cat1Value);
                            }
                        }
                    }, "2"
            );
        }
        return cat1View;
    }

    public com.couchbase.lite.View adsView() {
        this.adsView = this.getView("Ads");
        if (adsView.getMap() == null) {
            adsView.setMap(
                    new Mapper(){
                        @Override
                        public void map(Map<String, Object> document, Emitter emitter) {
                            String title = (String)document.get("title");
                            if(title!=null){
                                emitter.emit(document.get("_id"), title);
                            }
                        }
                    }, "2"
            );
        }
        return this.adsView;
    }


    public com.couchbase.lite.View deletedAdsView() {
        com.couchbase.lite.View deletedAdsView = this.getView("deletedAdsView");
        if (deletedAdsView.getMap() == null) {
            deletedAdsView.setMap(
                    new Mapper(){
                        @Override
                        public void map(Map<String, Object> document, Emitter emitter) {
                            boolean deleted = (boolean)document.get("deleted");
                            if(deleted){
                                emitter.emit(document.get("_id"), 1);
                            }
                        }
                    }, "1"
            );
        }
        return deletedAdsView;
    }

    public com.couchbase.lite.View adsViewFilters() {
        this.adsViewFilters = this.getView("AdsFilters");
        if (adsViewFilters.getMap() == null) {
            adsViewFilters.setMap(
                    new Mapper(){
                        @Override
                        public void map(Map<String, Object> document, Emitter emitter) {
                            ArrayList<String> filters = (ArrayList<String>)document.get("filters");
                            if(filters!=null){
                                emitter.emit(document.get("_id"), filters);
                            }
                        }
                    }, "3"
            );
        }
        return this.adsViewFilters;
    }

    private com.couchbase.lite.View getView(String name) {
        com.couchbase.lite.View view = null;
        try {
            view = DatabaseInstance.getInstance().database.getView(name);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return view;
    }

    public void deleteDatabase(){
        try {
            this.database.delete();
            this.manager=null;
            this.database=null;
            databaseInstance=null;
        }
        catch(CouchbaseLiteException e){
            return;
        }
    }
}
