package com.example.ivana.oglasi;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Revision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.example.ivana.oglasi.Classes.DatabaseInstance;
import com.example.ivana.oglasi.Classes.DropboxCredentials;
import com.example.ivana.oglasi.Classes.Helper;
import com.example.ivana.oglasi.Classes.ImageSaver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class EditAdActivity extends AppCompatActivity {

    EditText mEditAdTitle;
    EditText mEditAdText;
    Spinner mEditAdCat1;
    Spinner mEditAdCat2;
    Button mEditAd;
    Button mCancel;
    ImageButton mAddMoreImages;
    static final int[] LOOKUP_TABLE=new int[]{
            R.array.__,
            R.array.aksesoari,
            R.array.muska_odeca,
            R.array.zenska_odeca,
            R.array.kucni_ljubimci,
            R.array.kuce,
            R.array.stanovi,
            R.array.tehnika,
            R.array.ostalo
    };
    boolean spinner2InitialSet=true;
    int viewCounter=1;
    Hashtable<Integer,Uri> pendingImages;
    int PICK_IMAGE_MULTIPLE=1;
    String adId;
    LinearLayout mLinearLayout;
    boolean userLoaded=false;
    CloudStorage dropbox;
    ArrayList<String> pathsToUpload=new ArrayList<>();
    ArrayList<Uri> urisToUpload=new ArrayList<>();
    int imagesCounter=0;
    boolean imagesUploaded=false;
    ProgressDialog uploadProgress;
    Document updatedAd;
    Map<String, Object> mapAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_ad);

        invalidateOptionsMenu();

        CloudRail.setAppKey(DropboxCredentials.AppKey);
        dropbox = new Dropbox(getApplicationContext(), DropboxCredentials.API_ID, DropboxCredentials.API_KEY);

        mEditAdTitle=(EditText)findViewById(R.id.editText_editAdTitle);
        mEditAdText=(EditText)findViewById(R.id.editText_editAdText);
        mEditAdCat1=(Spinner)findViewById(R.id.spinner_editAdCat1);
        mEditAdCat2=(Spinner)findViewById(R.id.spinner_editAdCat2);
        mEditAd=(Button) findViewById(R.id.button_editAdConfirm);
        mCancel=(Button) findViewById(R.id.button_editAdCancel);
        mAddMoreImages=(ImageButton) findViewById(R.id.imageButton_addMoreImages);
        pendingImages=new Hashtable<>();

        adId = getIntent().getExtras().getString("ad_id");
        Document adDoc= DatabaseInstance.getInstance().database.getExistingDocument(adId);
        final Map<String, Object> adProperties=new HashMap<String, Object>();
        adProperties.putAll(adDoc.getProperties());

        mEditAdTitle.setText((String)adProperties.get("title"));
        mEditAdText.setText((String)adProperties.get("text"));

        final List<String> filters=(List<String>)adProperties.get("filters");

        String[] cat1Data=getResources().getStringArray(R.array.cat_1);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(EditAdActivity.this,android.R.layout.simple_spinner_dropdown_item,cat1Data);
        mEditAdCat1.setAdapter(adapter);
        for(int i=0;i<cat1Data.length;i++){
            if(cat1Data[i].equals(filters.get(0))){
                mEditAdCat1.setSelection(i);
                break;
            }
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(EditAdActivity.this);
        String username = preferences.getString("Username", "");

        final Document userDoc= DatabaseInstance.getInstance().database.getExistingDocument(username);
        Map<String, Object> userProperties=new HashMap<String, Object>();
        userProperties.putAll(userDoc.getProperties());

        final ArrayList<String> adImages=(ArrayList<String>) adProperties.get("images");

        mLinearLayout =(LinearLayout)findViewById(R.id.linearLayout_editImages);
        for(int i=0;i<adImages.size();i++){
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), adImages.get(i));
            Uri imgUri=Uri.fromFile(file);
            Bitmap bm= Helper.decodeSampledBitmapFromUri(imgUri,getApplicationContext(),200,200);
            final ImageView image=new ImageView(EditAdActivity.this);
            image.setImageBitmap(bm);

            RelativeLayout relativeLayout=new RelativeLayout(EditAdActivity.this);
            relativeLayout.setId(viewCounter++);
            RelativeLayout.LayoutParams pLayout=new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            relativeLayout.setLayoutParams(pLayout);
            relativeLayout.setPadding(0,30,0,0);

            View divider=new View(EditAdActivity.this);
            RelativeLayout.LayoutParams divParams=new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    2
            );
            divider.setBackgroundResource(R.color.lightGray);
            divParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            divParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            divParams.setMargins(0,0,0,15);
            int id0=viewCounter++;
            divider.setLayoutParams(divParams);
            divider.setId(id0);
            relativeLayout.addView(divider);

            int id1=viewCounter++;
            image.setId(id1);
            RelativeLayout.LayoutParams p1=new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            p1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            p1.addRule(RelativeLayout.BELOW,id0);
            image.setLayoutParams(p1);
            relativeLayout.addView(image);

            RelativeLayout.LayoutParams p2=new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            p2.addRule(RelativeLayout.BELOW,id1);
            ImageButton deleteImage=new ImageButton(EditAdActivity.this);
            Bitmap buttonImage=BitmapFactory.decodeResource(getResources(),R.mipmap.ic_delete_forever_black_24dp);
            deleteImage.setImageBitmap(buttonImage);
            deleteImage.setLayoutParams(p2);
            int id2=viewCounter++;
            deleteImage.setId(id2);
            relativeLayout.addView(deleteImage);
            deleteImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(Helper.isNetworkAvailable(getApplicationContext())){
                        int buttonId=v.getId();
                        Document ad=DatabaseInstance.getInstance().database.getExistingDocument(adId);
                        Map<String,Object> adProps=new HashMap<String, Object>();
                        adProps.putAll(ad.getProperties());
                        ArrayList<String> images=(ArrayList<String>)adProps.get("images");
                        String imageName=(String)((TextView)findViewById(buttonId+1)).getText();
                        final String path=adId+"/"+imageName;
                        images.remove(path);
                        boolean deleted=false;
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), path);
                        if(file.exists()){
                            deleted=Helper.deleteRecursive(file);
                        }

                        adProps.put("images",images);

                        new Thread(){
                            @Override
                            public void run(){
                                if(dropbox.exists("/Oglasi/"+path)){
                                    dropbox.delete("/Oglasi/"+path);
                                }
                            }
                        }.start();
                        try{
                            ad.putProperties(adProps);
                        }catch (CouchbaseLiteException e){
                            android.util.Log.e("Oglasi",e.getMessage());
                        }
                        RelativeLayout rl=(RelativeLayout)findViewById(buttonId-3);
                        ((ViewGroup)rl.getParent()).removeView(rl);
                    }
                    else{
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(EditAdActivity.this);
                        builder1.setMessage("Ne možete brisati slike ukoliko niste povezani na internet.");
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

            TextView imageName=new TextView(EditAdActivity.this);
            RelativeLayout.LayoutParams p3=new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            p3.addRule(RelativeLayout.BELOW,id1);
            p3.addRule(RelativeLayout.RIGHT_OF,id2);
            imageName.setLayoutParams(p3);
            int id3=viewCounter++;
            imageName.setId(id3);
            String[] folderAndImageName=adImages.get(i).split("/");
            imageName.setText(folderAndImageName[1]);
            relativeLayout.addView(imageName);

            mLinearLayout.addView(relativeLayout);
        }

        mEditAdCat1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(spinner2InitialSet){
                    String[] cat2Data=getResources().getStringArray(LOOKUP_TABLE[position]);
                    ArrayAdapter<String> adapter2=new ArrayAdapter<String>(EditAdActivity.this,android.R.layout.simple_spinner_dropdown_item,cat2Data);
                    mEditAdCat2.setAdapter(adapter2);
                    for(int j=0;j<cat2Data.length;j++){
                        if(cat2Data[j].equals(filters.get(1))){
                            mEditAdCat2.setSelection(j);
                            break;
                        }
                    }
                    spinner2InitialSet=false;
                }
                else{
                    String[] cat2Data=getResources().getStringArray(LOOKUP_TABLE[position]);
                    ArrayAdapter<String> adapter=new ArrayAdapter<String>(EditAdActivity.this,android.R.layout.simple_spinner_dropdown_item,cat2Data);
                    mEditAdCat2.setAdapter(adapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mAddMoreImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!Helper.isNetworkAvailable(getApplicationContext())){
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(EditAdActivity.this);
                    builder1.setMessage("Ne možete postavljati slike ukoliko niste povezani na internet.");
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
                else{
                    Intent intent = new Intent();
                    intent.setType("image/jpg");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    startActivityForResult(Intent.createChooser(intent,"Izaberite sliku"), PICK_IMAGE_MULTIPLE);
                }
            }
        });

        mEditAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean success=true;

                if(mEditAdTitle.getText().toString().trim().equalsIgnoreCase("")){
                    mEditAdTitle.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mEditAdTitle.setError(null);
                }

                if(mEditAdText.getText().toString().trim().equalsIgnoreCase("")){
                    mEditAdText.setError("Ovo polje je obavezno");
                    success=false;
                } else{
                    mEditAdText.setError(null);
                }

                if(mEditAdCat1.getSelectedItem().toString().equals("--")){
                    Toast.makeText(getApplicationContext(), "Morate izabrati kategoriju", Toast.LENGTH_LONG).show();
                    success=false;
                }

                if(mEditAdCat2.getSelectedItem().toString().equals("--")){
                    Toast.makeText(getApplicationContext(), "Morate izabrati potkategoriju", Toast.LENGTH_LONG).show();
                    success=false;
                }

                if(success){
                    updatedAd = DatabaseInstance.getInstance().database.getExistingDocument(adId);
                    mapAd = new HashMap<String, Object>();
                    mapAd.putAll(updatedAd.getProperties());
                    mapAd.put("title",mEditAdTitle.getText().toString());
                    mapAd.put("text",mEditAdText.getText().toString());
                    mapAd.put("updated_at",System.currentTimeMillis() / 1000L);

                    ArrayList<String> filters=new ArrayList<String>();
                    filters.add(mEditAdCat1.getSelectedItem().toString());
                    filters.add(mEditAdCat2.getSelectedItem().toString());
                    mapAd.put("filters",filters);

                    if(Helper.isNetworkAvailable(getApplicationContext())){
                        uploadProgress=new ProgressDialog(EditAdActivity.this);
                        uploadProgress.setTitle("Sačekajte");
                        uploadProgress.setMessage("Dodavanje slika...");
                        uploadProgress.setProgress(0);
                        uploadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        uploadProgress.setIndeterminate(true);
                        uploadProgress.show();

                        int num=4;
                        ArrayList<String> images=(ArrayList<String>)mapAd.get("images");
                        for(int i=0;i<pendingImages.size();i++){
                            while(!pendingImages.containsKey(num))
                                num+=5;

                            Uri imageUri=pendingImages.get(num);
                            Bitmap bm=Helper.decodeSampledBitmapFromUri(imageUri,getApplicationContext(),1000,1000);

                            String imageName=Helper.getFileName(getApplicationContext(),imageUri);
                            String path=adId+"/"+imageName;
                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), path);
                            if(!file.exists()){
                                new ImageSaver(getApplicationContext()).setDirectoryName(adId).setFileName(imageName).setExternal(true).save(bm);
                            }

                            images.add(path);

                            pathsToUpload.add(path);
                            urisToUpload.add(imageUri);

                            num+=5;
                        }

                        mapAd.put("images",images);

                        new Thread(){
                            @Override
                            public void run(){
                                for(int i=0;i<pathsToUpload.size();i++){
                                    Uri mImageUri=urisToUpload.get(i);
                                    if(mImageUri!=null){
                                        try {

                                            InputStream is = getApplicationContext().getContentResolver().openInputStream(mImageUri);
                                            long size=is.available();

                                            if(!dropbox.exists("/Oglasi/"+adId)){
                                                dropbox.createFolder("/Oglasi/"+adId);
                                            }
                                            if(dropbox.exists("/Oglasi/"+pathsToUpload.get(i))){
                                                dropbox.upload("/Oglasi/"+pathsToUpload.get(i),is,size,true);
                                            }
                                            else{
                                                dropbox.upload("/Oglasi/"+pathsToUpload.get(i),is,size,false);
                                            }

                                        } catch (Exception e) {
                                            android.util.Log.e("Oglasi",e.getMessage());
                                        }
                                    }
                                    final int pos=i;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            int jump=100/(pathsToUpload.size()-pos);
                                            uploadProgress.setProgress(jump);
                                        }
                                    });
                                }
                                imagesUploaded=true;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        uploadProgress.dismiss();
                                        try{
                                            updatedAd.putProperties(mapAd);
                                        }
                                        catch(CouchbaseLiteException e){
                                            Log.e("Oglasi",e.getMessage());
                                        }
                                        Intent intent=new Intent(EditAdActivity.this,AdActivity.class);
                                        intent.putExtra("AD_ID",adId);
                                        startActivity(intent);
                                    }
                                });
                            }
                        }.start();
                    }
                    else{
                        Toast.makeText(EditAdActivity.this,"Slike nisu dodate jer niste povezani na internet.",Toast.LENGTH_LONG).show();
                        try{
                            updatedAd.putProperties(mapAd);
                        }
                        catch(CouchbaseLiteException e){
                            Log.e("Oglasi",e.getMessage());
                        }
                        Intent intent=new Intent(EditAdActivity.this,AdActivity.class);
                        intent.putExtra("AD_ID",adId);
                        startActivity(intent);
                    }
                }
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(EditAdActivity.this,AdActivity.class);
                intent.putExtra("AD_ID",adId);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            // When an Image is picked
            if (requestCode == PICK_IMAGE_MULTIPLE && resultCode == RESULT_OK
                    && null != data) {

                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                if(data.getData()!=null){

                    Uri mImageUri=data.getData();

                    ImageView image=new ImageView(EditAdActivity.this);
                    Bitmap bm= Helper.decodeSampledBitmapFromUri(mImageUri,EditAdActivity.this,200,200);
                    image.setImageBitmap(bm);

                    RelativeLayout relativeLayout=new RelativeLayout(EditAdActivity.this);
                    relativeLayout.setId(viewCounter++);
                    RelativeLayout.LayoutParams pLayout=new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                    );
                    relativeLayout.setLayoutParams(pLayout);
                    relativeLayout.setPadding(0,30,0,0);

                    View divider=new View(EditAdActivity.this);
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
                    image.setId(id1);
                    RelativeLayout.LayoutParams p1=new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                    );
                    p1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    p1.addRule(RelativeLayout.BELOW,id0);
                    image.setLayoutParams(p1);
                    relativeLayout.addView(image);

                    RelativeLayout.LayoutParams p2=new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                    );
                    p2.addRule(RelativeLayout.BELOW,id1);
                    p2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    ImageButton deleteImage=new ImageButton(EditAdActivity.this);
                    Bitmap buttonImage=BitmapFactory.decodeResource(getResources(),R.mipmap.ic_delete_forever_black_24dp);
                    deleteImage.setImageBitmap(buttonImage);
                    deleteImage.setLayoutParams(p2);
                    int id2=viewCounter++;
                    deleteImage.setId(id2);
                    relativeLayout.addView(deleteImage);
                    deleteImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int buttonId=v.getId();
                            RelativeLayout rl=(RelativeLayout)findViewById(buttonId-3);
                            ((ViewGroup)rl.getParent()).removeView(rl);
                            pendingImages.remove(buttonId);
                        }
                    });

                    TextView imageName=new TextView(EditAdActivity.this);
                    RelativeLayout.LayoutParams p3=new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                    );
                    p3.addRule(RelativeLayout.LEFT_OF,id1);
                    p3.addRule(RelativeLayout.END_OF,id1);
                    p3.addRule(RelativeLayout.BELOW,id2);
                    imageName.setLayoutParams(p3);
                    int id3=viewCounter++;
                    imageName.setId(id3);
                    imageName.setText(Helper.getFileName(getApplicationContext(),mImageUri));
                    relativeLayout.addView(imageName);

                    pendingImages.put(id2,mImageUri);
                    mLinearLayout.addView(relativeLayout);

                }else {
                    if (data.getClipData() != null) {
                        ClipData mClipData = data.getClipData();
                        for (int i = 0; i < mClipData.getItemCount(); i++) {

                            ClipData.Item item = mClipData.getItemAt(i);
                            Uri uri = item.getUri();

                            ImageView image=new ImageView(EditAdActivity.this);
                            Bitmap bm= Helper.decodeSampledBitmapFromUri(uri,EditAdActivity.this,200,200);
                            image.setImageBitmap(bm);

                            RelativeLayout relativeLayout=new RelativeLayout(EditAdActivity.this);
                            relativeLayout.setId(viewCounter++);
                            RelativeLayout.LayoutParams pLayout=new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.MATCH_PARENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT
                            );
                            relativeLayout.setLayoutParams(pLayout);
                            relativeLayout.setPadding(0,30,0,0);

                            View divider=new View(EditAdActivity.this);
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
                            image.setId(id1);
                            RelativeLayout.LayoutParams p1=new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT
                            );
                            p1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                            p1.addRule(RelativeLayout.BELOW,id0);
                            image.setLayoutParams(p1);
                            relativeLayout.addView(image);

                            RelativeLayout.LayoutParams p2=new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT
                            );
                            p2.addRule(RelativeLayout.BELOW,id1);
                            p2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                            ImageButton deleteImage=new ImageButton(EditAdActivity.this);
                            Bitmap buttonImage=BitmapFactory.decodeResource(getResources(),R.mipmap.ic_delete_forever_black_24dp);
                            deleteImage.setImageBitmap(buttonImage);
                            deleteImage.setLayoutParams(p2);
                            int id2=viewCounter++;
                            deleteImage.setId(id2);
                            relativeLayout.addView(deleteImage);
                            deleteImage.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    int buttonId=v.getId();
                                    RelativeLayout rl=(RelativeLayout)findViewById(buttonId-3);
                                    ((ViewGroup)rl.getParent()).removeView(rl);
                                    pendingImages.remove(buttonId);
                                }
                            });

                            TextView imageName=new TextView(EditAdActivity.this);
                            RelativeLayout.LayoutParams p3=new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT
                            );
                            p3.addRule(RelativeLayout.BELOW,id1);
                            p3.addRule(RelativeLayout.RIGHT_OF,id2);
                            imageName.setLayoutParams(p3);
                            int id3=viewCounter++;
                            imageName.setId(id3);
                            imageName.setText(Helper.getFileName(getApplicationContext(),uri));
                            relativeLayout.addView(imageName);

                            pendingImages.put(id2,uri);
                            mLinearLayout.addView(relativeLayout);
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Niste izabrali sliku",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Greška", Toast.LENGTH_LONG)
                    .show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(EditAdActivity.this);
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
            Intent intent = new Intent(EditAdActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_user) {
            String username=PreferenceManager.getDefaultSharedPreferences(EditAdActivity.this).getString("Username", "");
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
            Intent intent = new Intent(EditAdActivity.this, UserActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(EditAdActivity.this, SearchActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_login) {
            Intent myIntent = new Intent(EditAdActivity.this,LoginActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_logout){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent intent = new Intent(EditAdActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(EditAdActivity.this,"Uspešno ste se odjavili sa svog naloga.",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(id==R.id.action_add){
            Intent myIntent = new Intent(EditAdActivity.this,NewAdActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_home){
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent myIntent = new Intent(EditAdActivity.this,HomeActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}