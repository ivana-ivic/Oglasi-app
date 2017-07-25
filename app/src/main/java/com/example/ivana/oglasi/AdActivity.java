package com.example.ivana.oglasi;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Revision;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.DefaultSliderView;
import com.example.ivana.oglasi.Classes.DatabaseInstance;
import com.example.ivana.oglasi.Classes.DropboxCredentials;
import com.example.ivana.oglasi.Classes.Helper;
import com.example.ivana.oglasi.Classes.ImageSaver;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AdActivity extends AppCompatActivity {

    public static final String TAG = "Oglasi";
    TextView mAdTitle;
    TextView mAdCategories1;
    TextView mAdCategories2;
    TextView mAdCategories3;
    TextView mAdDateTime;
    TextView mAdText;
    SliderLayout sliderShow;
    TextView mOwnerUsername;
    TextView mOwnerEmail;
    TextView mOwnerPhone;
    ImageButton mEditAd;
    ImageButton mDeleteAd;
    Replication pullCounter;
    boolean counterLoaded=false;
    boolean userLoaded=false;
    ImageButton mAddComment;
    String adId;
    Replication pullAd;
    boolean adLoaded=false;
    LinearLayout commentsLinearLayout;
    RelativeLayout relativeLayoutReport;
    RelativeLayout relativeLayoutDivider;
    List<String> adImages=new ArrayList<>();
    List<String> imagesToDownload=new ArrayList<>();
    CloudStorage dropbox;
    ProgressBar imagesLoading;
    boolean imagesDownloaded=false;
    Replication pullOwner;
    boolean ownerPulled;
    Document ownerUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad);

        invalidateOptionsMenu();

        CloudRail.setAppKey(DropboxCredentials.AppKey);
        dropbox = new Dropbox(AdActivity.this, DropboxCredentials.API_ID, DropboxCredentials.API_KEY);

        adId=getIntent().getExtras().getString("AD_ID");
        final Document adDoc= DatabaseInstance.getInstance().database.getExistingDocument(adId);
        final Map<String, Object> adProperties=new HashMap<String, Object>();
        adProperties.putAll(adDoc.getProperties());

        mEditAd =(ImageButton)findViewById(R.id.imageButton_editAd);
        mDeleteAd =(ImageButton)findViewById(R.id.imageButton_deleteAd);
        mAddComment=(ImageButton)findViewById(R.id.imageButton_addComment);
        imagesLoading=(ProgressBar)findViewById(R.id.progressBar_imagesLoading);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AdActivity.this);
        String username=preferences.getString("Username", "");
        if(username.isEmpty() || (!username.isEmpty() && !username.equals(adProperties.get("user_id")))){
            mEditAd.setVisibility(View.GONE);
            mDeleteAd.setVisibility(View.GONE);
        }
        if(username.isEmpty()){
            mAddComment.setVisibility(View.GONE);
        }

        mAddComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(AdActivity.this,AddCommentActivity.class);
                intent.putExtra("ad_id",adDoc.getId());
                startActivity(intent);
            }
        });

        commentsLinearLayout = (LinearLayout)findViewById(R.id.comments_linearLayout);

        relativeLayoutDivider=new RelativeLayout(getApplicationContext());
        RelativeLayout.LayoutParams pLayout=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        relativeLayoutDivider.setLayoutParams(pLayout);
        relativeLayoutDivider.setPadding(0,40,0,20);

        View divider=new View(getApplicationContext());
        RelativeLayout.LayoutParams divParams=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                2
        );
        divider.setBackgroundResource(R.color.lightGray);
        divParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        divParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        divider.setLayoutParams(divParams);
        divider.setPadding(0,0,0,15);
        relativeLayoutDivider.addView(divider);

        relativeLayoutReport=new RelativeLayout(getApplicationContext());
        relativeLayoutReport.setLayoutParams(pLayout);
        relativeLayoutReport.setPadding(0,0,0,20);

        TextView mReport=new TextView(getApplicationContext());
        RelativeLayout.LayoutParams pReport=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        pReport.addRule(RelativeLayout.ALIGN_PARENT_END);
        mReport.setLayoutParams(pReport);
        mReport.setText("Prijavi oglas");
        mReport.setTextSize(16);
        mReport.setTypeface(null,Typeface.ITALIC);
        mReport.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.darkRed));
        relativeLayoutReport.addView(mReport);
        mReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(AdActivity.this);
                builder1.setMessage("Da li ste sigurni da želite da prijavite ovaj oglas zbog neprimerenog sadržaja?");
                builder1.setCancelable(true);

                builder1.setPositiveButton("Da", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Document adDoc = DatabaseInstance.getInstance().database.getExistingDocument(getIntent().getExtras().getString("AD_ID"));
                        Map<String,Object> adProps=new HashMap<String, Object>();
                        adProps.putAll(adDoc.getProperties());
                        adProps.put("report_flag",true);
                        int reportCount=(int)adProps.get("report_count");
                        reportCount++;
                        adProps.put("report_count",reportCount);

                        try{
                            adDoc.putProperties(adProps);
                        } catch(Exception e){
                            Toast.makeText(AdActivity.this, "Niste uspeli da prijavite oglas.",Toast.LENGTH_LONG).show();
                        }

                        Toast.makeText(AdActivity.this, "Uspešno ste prijavili oglas.",Toast.LENGTH_LONG).show();
                        new ResolveAdConflictsTask().execute();
                    }
                });

                builder1.setNegativeButton(
                        "Ne",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert11 = builder1.create();
                alert11.show();
            }
        });

        mEditAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdActivity.this, EditAdActivity.class);
                intent.putExtra("ad_id", getIntent().getExtras().getString("AD_ID"));
                startActivity(intent);
            }
        });


        mDeleteAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(AdActivity.this);
                builder1.setMessage("Da li ste sigurni da želite da obrišete ovaj oglas?");
                builder1.setCancelable(true);

                builder1.setPositiveButton("Da", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Document ad= DatabaseInstance.getInstance().database.getExistingDocument(getIntent().getExtras().getString("AD_ID"));
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AdActivity.this);
                        String username = preferences.getString("Username", "");
//                        Document user= DatabaseInstance.getInstance().database.getExistingDocument(username);
//                        Map<String, Object> userProperties=new HashMap<String, Object>();
//                        userProperties.putAll(user.getProperties());
//                        ArrayList<String> ads=(ArrayList<String>)userProperties.get("ads");

                        Map<String,Object> adProperties=new HashMap<>();
                        adProperties.putAll(ad.getProperties());
                        adProperties.put("deleted",true);

                        //delete all images
                        final ArrayList<String> adImages=(ArrayList<String>)adProperties.get("images");
                        boolean deleted=false;
                        if(adImages.size()!=0){
                            String[] folderAndFileName=adImages.get(0).split("/");
                            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderAndFileName[0]);
                            if(dir.exists()){
                                deleted=Helper.deleteRecursive(dir);
                            }

                            if(Helper.isNetworkAvailable(getApplicationContext())){
                                new Thread(){
                                    @Override
                                    public void run(){
                                        String[] folderAndFileName=adImages.get(0).split("/");
                                        if(dropbox.exists("/Oglasi/"+folderAndFileName[0])){
                                            dropbox.delete("/Oglasi/"+folderAndFileName[0]);
                                        }
                                    }
                                }.start();
                            }
                        }

                        Document adsDeleted=DatabaseInstance.getInstance().database.getExistingDocument("ads_deleted");
                        Map<String,Object> adsDeletedProperties=new HashMap<String, Object>();
                        adsDeletedProperties.putAll(adsDeleted.getProperties());
                        ArrayList<String> deletedIds=(ArrayList<String>)adsDeletedProperties.get("ids");
                        deletedIds.add(ad.getId());
                        adsDeletedProperties.put("ids",deletedIds);

                        try{
                            adsDeleted.putProperties(adsDeletedProperties);
                            ad.putProperties(adProperties);
//                            ads.remove(ad.getId());
//                            userProperties.put("ads",ads);
//                            user.putProperties(userProperties);
                        } catch(Exception e){
                            return;
                        }

                        Helper.resolveAdConflicts(ad.getId());

                        new ResolveAdCounterConflictsTask().execute();
                    }
                });

                builder1.setNegativeButton(
                        "Ne",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert11 = builder1.create();
                alert11.show();
            }
        });

        adImages=(ArrayList<String>)adProperties.get("images");
        sliderShow = (SliderLayout) findViewById(R.id.slider);

        if(adImages.size()==0){
            Uri uri=Uri.parse("android.resource://com.example.ivana.oglasi/drawable/no_image");
            DefaultSliderView dsv=new DefaultSliderView(this);
            dsv.image(uri.toString());
            sliderShow.addSlider(dsv);
            imagesLoading.setVisibility(View.GONE);
        }
        else{
            for(int i=0;i<adImages.size();i++){
                DefaultSliderView dsv=new DefaultSliderView(this);
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), adImages.get(i));
                if(file.exists()){
                    dsv.image(file);
                    dsv.description(adImages.get(i));
                    sliderShow.addSlider(dsv);
                    dsv.setOnSliderClickListener(new BaseSliderView.OnSliderClickListener() {
                        @Override
                        public void onSliderClick(BaseSliderView slider) {
                            Intent fullScreenIntent = new Intent(AdActivity.this, FullScreenImageActivity.class);
                            fullScreenIntent.putExtra("file_name", slider.getDescription());
                            startActivity(fullScreenIntent);
                        }
                    });
                }
                else{
                    imagesToDownload.add(adImages.get(i));
                }
            }

            sliderShow.startAutoCycle();
        }

        mAdTitle=(TextView)findViewById(R.id.textView_AdTitle);
        mAdCategories1=(TextView)findViewById(R.id.textView_AdCategories1);
        mAdCategories2=(TextView)findViewById(R.id.textView_AdCategories2);
        mAdCategories3=(TextView)findViewById(R.id.textView_AdCategories3);
        mAdDateTime=(TextView)findViewById(R.id.textView_AdDateTime);
        mAdText=(TextView)findViewById(R.id.textView_AdText);

        mAdCategories2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(AdActivity.this,SearchActivity.class);
                intent.putExtra("cat1",mAdCategories2.getText().toString());
                startActivity(intent);
            }
        });

        mAdCategories3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(AdActivity.this,SearchActivity.class);
                intent.putExtra("cat1",mAdCategories2.getText().toString());
                intent.putExtra("cat2",mAdCategories3.getText().toString());
                startActivity(intent);
            }
        });

        mAdTitle.setText((String)adProperties.get("title"));

        List<String> categories = (List<String>)adProperties.get("filters");

        mAdCategories1.setText("Svi oglasi");
        mAdCategories2.setText(categories.get(0));
        mAdCategories3.setText(categories.get(1));

        long timestampLong = (long)((int)adProperties.get("created_at"))*1000;
        Date dateAndTime = new java.util.Date(timestampLong);
        String dateAndTimeString = new SimpleDateFormat("dd/MM/yyyy, HH:mm").format(dateAndTime);
        mAdDateTime.setText(dateAndTimeString+"h");
        mAdText.setText((String)adProperties.get("text"));

        mOwnerUsername=(TextView)findViewById(R.id.textView_ownerUser);
        mOwnerEmail=(TextView)findViewById(R.id.textView_ownerUserEmail);
        mOwnerPhone=(TextView)findViewById(R.id.textView_ownerUserPhone);

        ownerUser = DatabaseInstance.getInstance().database.getExistingDocument((String)adProperties.get("user_id"));
        Map<String, Object> ownerUserProperties=new HashMap<String, Object>();
        if(ownerUser!=null){
            ownerUserProperties.putAll(ownerUser.getProperties());
            mOwnerUsername.setText((String)ownerUserProperties.get("_id"));
            mOwnerEmail.setText((String)ownerUserProperties.get("email"));
            mOwnerPhone.setText((String)ownerUserProperties.get("phone"));
        }
        else{
            mOwnerUsername.setText((String)adProperties.get("user_id"));
            mOwnerEmail.setText("Korisnik nije učitan");
            mOwnerPhone.setText("Korisnik nije učitan");
        }

        mOwnerUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ownerUser!=null){
                    Intent intent = new Intent(AdActivity.this, UserActivity.class);
                    intent.putExtra("user_id", mOwnerUsername.getText().toString());
                    startActivity(intent);
                }
                else{
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(AdActivity.this);
                    builder1.setTitle("Korisnik");
                    builder1.setMessage("Informacije o korisniku ne postoje jer nisu učitane. Povežite se na internet kako biste učitali informacije o korisniku.");
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
        });

        if(imagesToDownload.size()>0){
            if(Helper.isNetworkAvailable(getApplicationContext())){
                Toast.makeText(getApplicationContext(),"Slike oglasa se učitavaju...",Toast.LENGTH_LONG);
                new Thread(){
                    @Override
                    public void run(){
                        try{
                            for(int i=0;i<imagesToDownload.size();i++){
                                String[] folderAndName=imagesToDownload.get(i).split("/");
                                if(dropbox.exists("/Oglasi/"+imagesToDownload.get(i))){
                                    InputStream result=dropbox.download("/Oglasi/"+imagesToDownload.get(i));
                                    Bitmap bm= BitmapFactory.decodeStream(result);
                                    new ImageSaver(getApplicationContext()).setExternal(true).setDirectoryName(folderAndName[0]).setFileName(folderAndName[1]).save(bm);
                                    final int pos=i;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            DefaultSliderView dsv=new DefaultSliderView(AdActivity.this);
                                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), imagesToDownload.get(pos));
                                            if(file.exists()){
                                                dsv.image(file);
                                                dsv.description(imagesToDownload.get(pos));
                                                sliderShow.addSlider(dsv);
                                                dsv.setOnSliderClickListener(new BaseSliderView.OnSliderClickListener() {
                                                    @Override
                                                    public void onSliderClick(BaseSliderView slider) {
                                                        Intent fullScreenIntent = new Intent(AdActivity.this, FullScreenImageActivity.class);
                                                        fullScreenIntent.putExtra("file_name", slider.getDescription());
                                                        startActivity(fullScreenIntent);
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            }
                        }
                        catch(Exception e){
                            Log.e("Oglasi",e.getMessage());
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imagesLoading.setVisibility(View.GONE);
                            }
                        });
                    }
                }.start();
                Toast.makeText(getApplicationContext(),"Slike oglasa su uspešno učitane.",Toast.LENGTH_LONG);
            }
            else{
                if(imagesToDownload.size()==adImages.size()){
                    DefaultSliderView dsv=new DefaultSliderView(AdActivity.this);
                    String[] folderAndName=adImages.get(0).split("/");
                    String path=folderAndName[0]+"/thumbnail_"+folderAndName[1];
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), path);
                    if(file.exists()){
                        dsv.image(file);
                        dsv.description(path);
                        sliderShow.addSlider(dsv);
                        dsv.setOnSliderClickListener(new BaseSliderView.OnSliderClickListener() {
                            @Override
                            public void onSliderClick(BaseSliderView slider) {
                                Intent fullScreenIntent = new Intent(AdActivity.this, FullScreenImageActivity.class);
                                fullScreenIntent.putExtra("file_name", slider.getDescription());
                                startActivity(fullScreenIntent);
                            }
                        });
                    }
                }
                imagesLoading.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onStop() {
        sliderShow.stopAutoCycle();
        super.onStop();
    }

    @Override
    protected void onResume(){
        super.onResume();
        new ResolveAdConflictsTask().execute();
    }

    public class ResolveAdCounterConflictsTask extends AsyncTask<Void,Void,Boolean> {
        protected Boolean doInBackground(Void... params) {
            if(Helper.isNetworkAvailable(getApplicationContext())){
                try {
                    URL url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                    pullCounter = DatabaseInstance.getInstance().database.createPullReplication(url);
                    pullCounter.setContinuous(false);
                    Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                    pullCounter.setAuthenticator(auth);
                    List<String> docIds=new ArrayList<>();
                    docIds.add("ads_counter");
                    pullCounter.setDocIds(docIds);
                    if(Helper.isNetworkAvailable(getApplicationContext())){
                        pullCounter.start();

                        pullCounter.addChangeListener(new Replication.ChangeListener() {
                            @Override
                            public void changed(Replication.ChangeEvent event) {
                                boolean active = (pullCounter.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                                if (!active) {
                                    counterLoaded=true;
                                }
                            }
                        });
                        while(!counterLoaded){
                            //wait...
                        }
                    }
                    else{
                        Toast.makeText(AdActivity.this, "Niste povezani na internet. Oglasi koje vidite nisu ažurirani.",Toast.LENGTH_LONG).show();
                    }
                    Helper.resolveAdCounterConflicts();
                } catch (Exception e) {
                    Log.e("Oglasi", e.getMessage());
                    return false;
                }
            }

            return true;
        }

        protected void onPostExecute(final Boolean success) {
            new ResolveAdConflictsTask().execute();
        }
    }

    public class ResolveAdConflictsTask extends AsyncTask<Void,Void,Boolean> {
        protected Boolean doInBackground(Void... params) {
            if(Helper.isNetworkAvailable(getApplicationContext())){
                try {
                    URL url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                    pullAd = DatabaseInstance.getInstance().database.createPullReplication(url);
                    pullAd.setContinuous(false);
                    Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                    pullAd.setAuthenticator(auth);
                    List<String> docIds=new ArrayList<>();
                    docIds.add(adId);
                    pullAd.setDocIds(docIds);
                    pullAd.start();
                } catch (Exception e) {
                    Log.e("Oglasi", e.getMessage());
                }

                pullAd.addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(Replication.ChangeEvent event) {
                        boolean active = (pullAd.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                        if (!active) {
                            adLoaded=true;
                        }
                    }
                });

                while(!adLoaded){
                    //wait...
                }
            }
            else{
                return false;
            }

            Helper.resolveAdConflicts(adId);
            return true;
        }

        protected void onPostExecute(final Boolean success) {
            if(DatabaseInstance.getInstance().database.getExistingDocument(adId)==null || (boolean)DatabaseInstance.getInstance().database.getExistingDocument(adId).getProperties().get("deleted")==true){
                AlertDialog.Builder builder1 = new AlertDialog.Builder(AdActivity.this);
                builder1.setMessage("Ovaj oglas više ne postoji. Bićete preusmereni na početnu stranu.");
                builder1.setCancelable(true);

                builder1.setNeutralButton(
                        "Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
                                Intent intent=new Intent(AdActivity.this,HomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                            }
                        });

                AlertDialog alert11 = builder1.create();
                alert11.show();
            }
            else{
                if(!success){
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(AdActivity.this);
                    builder1.setMessage("Niste povezani na internet. Oglas koji vidite možda nije ažuran.");
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

                int viewCounter=1;
                if(commentsLinearLayout.getChildCount() > 0)
                    commentsLinearLayout.removeAllViews();
                Document adDoc= DatabaseInstance.getInstance().database.getExistingDocument(adId);
                Map<String, Object> adProperties=new HashMap<String, Object>();
                adProperties.putAll(adDoc.getProperties());

                mAdTitle.setText((String)adProperties.get("title"));
                List<String> categories = (List<String>)adProperties.get("filters");
                mAdCategories1.setText("Svi oglasi");
                mAdCategories2.setText(categories.get(0));
                mAdCategories3.setText(categories.get(1));
                mAdText.setText((String)adProperties.get("text"));

                if(((ArrayList<Object>)adProperties.get("comments")).size()==0){
                    RelativeLayout relativeLayout=new RelativeLayout(getApplicationContext());
                    RelativeLayout.LayoutParams pLayout=new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                    );
                    relativeLayout.setLayoutParams(pLayout);
                    relativeLayout.setPadding(0,30,0,30);

                    View divider=new View(getApplicationContext());
                    RelativeLayout.LayoutParams divParams=new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            2
                    );
                    divider.setBackgroundResource(R.color.lightGray);
                    divParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    divParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                    int id0=viewCounter++;
                    divParams.setMargins(0,0,0,15);
                    divider.setLayoutParams(divParams);
                    divider.setId(id0);
                    relativeLayout.addView(divider);

                    int id1=viewCounter++;
                    TextView mUser=new TextView(getApplicationContext());
                    mUser.setId(id1);
                    RelativeLayout.LayoutParams p1=new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                    );
                    p1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    p1.addRule(RelativeLayout.BELOW,id0);
                    mUser.setLayoutParams(p1);
                    mUser.setText("Nema komentara");
                    mUser.setTypeface(null,Typeface.ITALIC);
                    mUser.setTextSize(18);
                    relativeLayout.addView(mUser);
                    commentsLinearLayout.addView(relativeLayout);
                }
                else{
                    ArrayList<Map<String,Object>> comments=(ArrayList<Map<String,Object>>)adProperties.get("comments");
                    for(int i=0;i<comments.size();i++){
                        Map<String, Object> comment=new HashMap<String, Object>();
                        comment.putAll(comments.get(i));
                        long timestampLong = (long)comment.get("timestamp");
                        Date dateAndTime = new java.util.Date(timestampLong);
                        String dateAndTimeString = new SimpleDateFormat("dd/MM/yyyy, HH:mm").format(dateAndTime);
                        dateAndTimeString+="h";

                        RelativeLayout relativeLayout=new RelativeLayout(getApplicationContext());
                        RelativeLayout.LayoutParams pLayout=new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                        );
                        relativeLayout.setLayoutParams(pLayout);
                        relativeLayout.setPadding(0,30,0,30);

                        View divider=new View(getApplicationContext());
                        RelativeLayout.LayoutParams divParams=new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                2
                        );
                        divider.setBackgroundResource(R.color.lightGray);
                        divParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        divParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                        int id0=viewCounter++;
                        divParams.setMargins(0,0,0,15);
                        divider.setLayoutParams(divParams);
                        divider.setId(id0);
                        relativeLayout.addView(divider);

                        int id1=viewCounter++;
                        TextView mUser=new TextView(getApplicationContext());
                        mUser.setId(id1);
                        RelativeLayout.LayoutParams p1=new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                        );
                        p1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        p1.addRule(RelativeLayout.BELOW,id0);
                        mUser.setLayoutParams(p1);
                        mUser.setText(comment.get("user").toString());
                        mUser.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.darkGreen));
                        mUser.setTypeface(Typeface.DEFAULT_BOLD);
                        mUser.setTextSize(16);
                        relativeLayout.addView(mUser);
                        mUser.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String username=((TextView)v).getText().toString();

                                try {
                                    URL url = new URL(DatabaseInstance.address + DatabaseInstance.DB_NAME);
                                    Replication pullCommUser = DatabaseInstance.getInstance().database.createPullReplication(url);
                                    pullCommUser.setContinuous(false);
                                    Authenticator auth = new BasicAuthenticator(DatabaseInstance.databaseUsername, DatabaseInstance.databasePassword);
                                    pullCommUser.setAuthenticator(auth);
                                    List<String> docIds=new ArrayList<>();
                                    docIds.add(username);
                                    pullCommUser.setDocIds(docIds);
                                    pullCommUser.start();

                                    while(DatabaseInstance.getInstance().database.getExistingDocument(username)==null){
                                        //wait...
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error occurred", e);
                                }

                                Intent intent=new Intent(AdActivity.this,UserActivity.class);
                                intent.putExtra("user_id",username);
                                startActivity(intent);
                            }
                        });

                        TextView mTime=new TextView(getApplicationContext());
                        int id2=viewCounter++;
                        mTime.setId(id2);
                        RelativeLayout.LayoutParams p2=new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                        );
                        p2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        p2.addRule(RelativeLayout.BELOW,id0);
                        mTime.setLayoutParams(p2);
                        mTime.setText(dateAndTimeString);
                        mTime.setTextSize(12);
                        mTime.setTypeface(null,Typeface.ITALIC);
                        relativeLayout.addView(mTime);

                        TextView mText=new TextView(getApplicationContext());
                        int id3=viewCounter++;
                        mText.setId(id3);
                        RelativeLayout.LayoutParams p3=new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                        );
                        p3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        p3.addRule(RelativeLayout.BELOW,id1);
                        mText.setLayoutParams(p3);
                        mText.setText(comment.get("text").toString());
                        mText.setTextSize(14);
                        mText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.darkGray));
                        relativeLayout.addView(mText);

                        commentsLinearLayout.addView(relativeLayout);
                    }
                }
                commentsLinearLayout.addView(relativeLayoutDivider);
                commentsLinearLayout.addView(relativeLayoutReport);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AdActivity.this);
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
            Intent intent = new Intent(AdActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_user) {
            String username=PreferenceManager.getDefaultSharedPreferences(AdActivity.this).getString("Username", "");
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
                    Log.e(TAG, "Error occurred", e);
                }
            }
            Helper.resolveUserConflicts(username);
            Intent intent = new Intent(AdActivity.this, UserActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(AdActivity.this, SearchActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_login) {
            Intent myIntent = new Intent(AdActivity.this,LoginActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_logout){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent intent = new Intent(AdActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(AdActivity.this,"Uspešno ste se odjavili sa svog naloga.",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(id==R.id.action_add){
            Intent myIntent = new Intent(AdActivity.this,NewAdActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_home){
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent myIntent = new Intent(AdActivity.this,HomeActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
