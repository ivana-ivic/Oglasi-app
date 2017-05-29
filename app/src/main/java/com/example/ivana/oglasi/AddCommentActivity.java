package com.example.ivana.oglasi;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.TransactionalTask;
import com.couchbase.lite.UnsavedRevision;
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

public class AddCommentActivity extends Activity {

    EditText mComment;
    Button mAddComment;
    boolean success=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_comment);

        DisplayMetrics dm=new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width=dm.widthPixels;
        int height=dm.heightPixels;
        getWindow().setLayout((int)(width*0.8),(int)(height*0.45));

        mComment=(EditText) findViewById(R.id.editText_comment);
        mAddComment=(Button)findViewById(R.id.button_addComment);

        mAddComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mComment.getText().toString().trim().equalsIgnoreCase("")){
                    mComment.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mComment.setError(null);
                    success=true;
                }
                if(success){
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AddCommentActivity.this);
                    String username=preferences.getString("Username", "");
                    final String adId=getIntent().getStringExtra("ad_id");
                    Document adDoc= DatabaseInstance.getInstance().database.getExistingDocument(adId);
                    Map<String,Object> adProperties=new HashMap<>();
                    adProperties.putAll(adDoc.getProperties());
                    String commentText=mComment.getText().toString();

                    Map<String, Object> comment=new HashMap<String, Object>();
                    comment.put("timestamp",System.currentTimeMillis());
                    comment.put("user",username);
                    comment.put("text",commentText);

                    ArrayList<Object> comments=(ArrayList<Object>)adProperties.get("comments");
                    comments.add(comment);
                    adProperties.put("comments",comments);

                    try{
                        adDoc.putProperties(adProperties);
                    }
                    catch (CouchbaseLiteException e){
                        Toast.makeText(AddCommentActivity.this, "Došlo je do greške.",Toast.LENGTH_LONG).show();
                    }

                    Intent adIntent=new Intent(AddCommentActivity.this,AdActivity.class);
                    adIntent.putExtra("AD_ID",adId);
                    adIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    finish();
                    startActivity(adIntent);
                }
            }
        });
    }
}
