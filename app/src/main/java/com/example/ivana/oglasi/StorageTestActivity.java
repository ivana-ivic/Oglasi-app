package com.example.ivana.oglasi;

import android.content.ClipData;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.example.ivana.oglasi.Classes.DatabaseInstance;
import com.example.ivana.oglasi.Classes.Helper;
import com.example.ivana.oglasi.Classes.HomeListAdapter;
import com.example.ivana.oglasi.Classes.HomeListItemData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StorageTestActivity extends AppCompatActivity {

    Button testStorage;
    Button testPull;
    Button testPush;
    int PICK_IMAGE_MULTIPLE=1;
    long size;
    InputStream is;

    //filtered pull by cat1
    public static AndroidContext androidContext;
    URL url;
    Replication pull;
    boolean pulledCat1=false;

    List<DocumentChange> documentChanges=new ArrayList<DocumentChange>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_test);

        androidContext=new AndroidContext(this);

        CloudRail.setAppKey("5911ab1dff21b5017c86daaa");

        testStorage=(Button)findViewById(R.id.button_testStorage);
        testPull=(Button)findViewById(R.id.button_testPull);
        testPush=(Button)findViewById(R.id.button_testPush);

        testStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/jpg");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent,"Izaberite sliku"), PICK_IMAGE_MULTIPLE);

            }
        });

        testPull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Document randomDoc=DatabaseInstance.getInstance(new AndroidContext(StorageTestActivity.this)).database.getDocument("testDoc");
                DatabaseInstance.getInstance().deleteDatabase();
                int docNum=DatabaseInstance.getInstance().database.getDocumentCount();
                try {
                    url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                    pull = DatabaseInstance.getInstance().database.createPullReplication(url);
                    pull.setContinuous(false);
                    Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                    pull.setAuthenticator(auth);
                    Map<String,Object> params=new HashMap<String, Object>();
                    params.put("cat1","Tehnika");
                    pull.setFilterParams(params);
                    pull.start();

                    pull.addChangeListener(new Replication.ChangeListener() {
                        @Override
                        public void changed(Replication.ChangeEvent event) {
                            boolean active=pull.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE;
                            if(!active)
                                pulledCat1=true;
                        }
                    });

                    while(!pulledCat1){
                        //wait...
                    }

                    docNum=DatabaseInstance.getInstance().database.getDocumentCount();
                    String cat1;
                    String cat2;
                    Query queryData = DatabaseInstance.getInstance().cat1View("Tehnika").createQuery();
                    queryData.setMapOnly(true);
                    queryData.setDescending(true);
                    QueryEnumerator result = queryData.run();
                    for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                        QueryRow row = it.next();
                        cat1 = (String) row.getKey();
                        cat2 = (String) row.getValue();
                    }

                } catch (Exception e) {
                    com.couchbase.lite.util.Log.e("Oglasi", e.getMessage());
                }
            }
        });

        testPush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                boolean deleted=false;
//                Map<String,Object> deletedLocally=DatabaseInstance.getInstance().database.getExistingLocalDocument("b786d50091ddccf55327c1f24707a461");
//                if(deletedLocally!=null){
//                    if(deletedLocally.containsKey("_deleted"))
//                        deleted=(boolean)deletedLocally.get("_deleted");
//                }

                DatabaseInstance.getInstance().database.addChangeListener(new Database.ChangeListener() {
                    @Override
                    public void changed(Database.ChangeEvent event) {
                        documentChanges.addAll(event.getChanges());
                    }
                });

                ArrayList<String> docs=new ArrayList<String>();
                Query q=DatabaseInstance.getInstance().deletedAdsView().createQuery();
                q.setIncludeDeleted(true);
//                q.setMapOnly(true);
                try{
                    QueryEnumerator result = q.run();
                    for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                        QueryRow row = it.next();
                        docs.add(row.getDocumentId());
                    }
                }
                catch(CouchbaseLiteException e){
                    Log.e("Oglasi",e.getMessage());
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int resRequestCode, int resResultCode, Intent resData) {
        final CloudStorage dropbox = new Dropbox(StorageTestActivity.this, "ygrvukjpli1fs6p", "qd6c11dan34caef");
        final Intent data=resData;
        final int requestCode=resRequestCode;
        final int resultCode=resResultCode;

        new Thread(){
            @Override
            public void run(){
//                dropbox.createFolder("/Oglasi/petra");
                // When an Image is picked
                if (requestCode == PICK_IMAGE_MULTIPLE && resultCode == RESULT_OK
                        && null != data) {

                    String[] filePathColumn = { MediaStore.Images.Media.DATA };
                    if(data.getData()!=null){

                        Uri mImageUri=data.getData();
                        try {
                            is = getApplicationContext().getContentResolver().openInputStream(mImageUri);
                            size=is.available();
                        } catch (Exception e) {
                            Toast.makeText(StorageTestActivity.this, "Greška", Toast.LENGTH_LONG)
                                    .show();
                        }

                        String fileName=Helper.getFileName(StorageTestActivity.this,mImageUri);
                        if(dropbox.exists("/Oglasi/petra/"+fileName)){
                            dropbox.upload("/Oglasi/petra/"+fileName,is,size,true);
                        }
                        else{
                            dropbox.upload("/Oglasi/petra/"+fileName,is,size,false);
                        }

                    }else {
                        if (data.getClipData() != null) {
                            ClipData mClipData = data.getClipData();
                            for (int i = 0; i < mClipData.getItemCount(); i++) {

                                ClipData.Item item = mClipData.getItemAt(i);
                                Uri uri = item.getUri();

                                try {
                                    is = getApplicationContext().getContentResolver().openInputStream(uri);
                                    size=is.available();
                                } catch (Exception e) {
                                    Toast.makeText(StorageTestActivity.this, "Greška", Toast.LENGTH_LONG)
                                            .show();
                                }
                                String fileName=Helper.getFileName(StorageTestActivity.this,uri);
                                if(dropbox.exists("/Oglasi/petra/"+fileName)){
                                    dropbox.upload("/Oglasi/petra/"+fileName,is,size,true);
                                }
                                else{
                                    dropbox.upload("/Oglasi/petra/"+fileName,is,size,false);
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(StorageTestActivity.this, "Niste izabrali sliku",
                            Toast.LENGTH_LONG).show();
                }
            }
        }.start();

        super.onActivityResult(requestCode, resultCode, data);
    }
}
