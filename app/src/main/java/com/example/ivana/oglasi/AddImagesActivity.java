package com.example.ivana.oglasi;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.UnsavedRevision;
import com.example.ivana.oglasi.Classes.DatabaseInstance;
import com.example.ivana.oglasi.Classes.DropboxCredentials;
import com.example.ivana.oglasi.Classes.Helper;
import com.example.ivana.oglasi.Classes.ImageSaver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddImagesActivity extends AppCompatActivity {

    ArrayList<Uri> mArrayUri;
    int PICK_IMAGE_MULTIPLE = 1;
    Button mSkip;
    ImageButton mSelectImages;
    LinearLayout mLinearAddImages;
    int viewCounter=1;
    String adId;
    Uri[] imageURIs=new Uri[20];
    CloudStorage dropbox;
    int uriCounter=0;
    RelativeLayout initialLayout;
    ProgressDialog uploadProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_images);

        invalidateOptionsMenu();

        CloudRail.setAppKey(DropboxCredentials.AppKey);
        dropbox = new Dropbox(getApplicationContext(), DropboxCredentials.API_ID, DropboxCredentials.API_KEY);

        mLinearAddImages=(LinearLayout)findViewById(R.id.linear_add_images);
        mArrayUri = new ArrayList<>();
        adId = getIntent().getExtras().getString("ad_id");

        initialLayout=new RelativeLayout(getApplicationContext());
        RelativeLayout.LayoutParams initialParams=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        initialLayout.setLayoutParams(initialParams);

        mSelectImages=new ImageButton(AddImagesActivity.this);
        mSelectImages.setId(R.id.imageButton_selectImages);
        RelativeLayout.LayoutParams selectImagesParams=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        selectImagesParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        selectImagesParams.addRule(RelativeLayout.ALIGN_BOTTOM,R.id.button_skipAddImages);
        mSelectImages.setLayoutParams(selectImagesParams);
        mSelectImages.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_add_a_photo_black_24dp));

        mSkip=new Button(AddImagesActivity.this);
        mSkip.setId(R.id.button_skipAddImages);
        RelativeLayout.LayoutParams skipParams=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        skipParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        skipParams.addRule(RelativeLayout.RIGHT_OF,R.id.imageButton_selectImages);
        skipParams.addRule(RelativeLayout.END_OF,R.id.imageButton_selectImages);
        mSkip.setLayoutParams(skipParams);
        mSkip.setText(R.string.new_ad_confirm);

        initialLayout.addView(mSelectImages);
        initialLayout.addView(mSkip);
        mLinearAddImages.addView(initialLayout);

        mSelectImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!Helper.isNetworkAvailable(getApplicationContext())){
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(AddImagesActivity.this);
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

        mSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(uriCounter==0){
                    Intent intent=new Intent(AddImagesActivity.this,UserAdsActivity.class);
                    startActivity(intent);
                }
                else if(!Helper.isNetworkAvailable(getApplicationContext())){
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(AddImagesActivity.this);
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
                    if(uriCounter!=0){
                        uploadProgress=new ProgressDialog(AddImagesActivity.this);
                        uploadProgress.setTitle("Sačekajte");
                        uploadProgress.setMessage("Dodavanje slika...");
                        uploadProgress.setProgress(0);
                        uploadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        uploadProgress.setIndeterminate(true);
                        uploadProgress.show();

                        final Document adDoc= DatabaseInstance.getInstance().database.getExistingDocument(adId);
                        Map<String,Object> adProps=new HashMap<>();
                        adProps.putAll(adDoc.getProperties());
                        List<String> imagesLinks=(ArrayList<String>)adProps.get("images");

                        for(int i=0;i<uriCounter;i++) {
                            Uri mImageUri=imageURIs[i];
                            if(mImageUri!=null){
                                String fileName=Helper.getFileName(AddImagesActivity.this,mImageUri);
                                Bitmap bm=Helper.decodeSampledBitmapFromUri(mImageUri,AddImagesActivity.this,300,300);

                                //save image locally
                                new ImageSaver(getApplicationContext()).setFileName(fileName).setDirectoryName(adId).setExternal(true).save(bm);

                                imagesLinks.add(adId+"/"+fileName);
                            }
                        }

                        adProps.put("images",imagesLinks);
                        try{
                            adDoc.putProperties(adProps);
                        }
                        catch (CouchbaseLiteException e){
                            Log.e("Oglasi",e.getMessage());
                        }

                        new Thread(){
                            @Override
                            public void run(){

                                for(int i=0;i<uriCounter;i++){
                                    Uri mImageUri=imageURIs[i];
                                    if(mImageUri!=null){
                                        try {
                                            String fileName=Helper.getFileName(AddImagesActivity.this,mImageUri);

                                            InputStream is = getApplicationContext().getContentResolver().openInputStream(mImageUri);
                                            long size=is.available();

                                            if(!dropbox.exists("/Oglasi/"+adId)){
                                                dropbox.createFolder("/Oglasi/"+adId);
                                            }
                                            if(dropbox.exists("/Oglasi/"+adId+"/"+fileName)){
                                                dropbox.upload("/Oglasi/"+adId+"/"+fileName,is,size,true);
                                            }
                                            else{
                                                dropbox.upload("/Oglasi/"+adId+"/"+fileName,is,size,false);
                                            }

                                        } catch (Exception e) {
                                            Log.e("Oglasi",e.getMessage());
                                        }
                                    }
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        uploadProgress.dismiss();

                                        Intent intent=new Intent(AddImagesActivity.this,UserAdsActivity.class);
                                        startActivity(intent);
                                    }
                                });
                            }
                        }.start();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            // When an Image is picked
            if (requestCode == PICK_IMAGE_MULTIPLE && resultCode == RESULT_OK && null != data){
                if(data.getData()!=null){
                    imageURIs[uriCounter++]=data.getData();
                }else {
                    if (data.getClipData() != null) {
                        ClipData mClipData = data.getClipData();
                        for (int i = 0; i < mClipData.getItemCount(); i++) {
                            imageURIs[uriCounter++]=mClipData.getItemAt(i).getUri();
                        }
                    }
                }

                mLinearAddImages.removeAllViews();
                mLinearAddImages.addView(initialLayout);

                for(int i=0;i<uriCounter;i++){

                    Uri mImageUri=imageURIs[i];
                    if(mImageUri!=null){
                        Bitmap bm=Helper.decodeSampledBitmapFromUri(mImageUri,AddImagesActivity.this,200,200);

                        String name=getFileName(mImageUri);

                        ImageView image=new ImageView(AddImagesActivity.this);
                        image.setImageBitmap(bm);

                        RelativeLayout relativeLayout=new RelativeLayout(AddImagesActivity.this);
                        relativeLayout.setId(viewCounter++);
                        RelativeLayout.LayoutParams pLayout=new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                        );
                        relativeLayout.setLayoutParams(pLayout);
                        relativeLayout.setPadding(0,30,0,0);

                        View divider=new View(AddImagesActivity.this);
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
                        p2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        p2.addRule(RelativeLayout.BELOW,id1);
                        ImageButton deleteImage=new ImageButton(AddImagesActivity.this);
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
                                int imageId=buttonId+2;
                                int num =Integer.parseInt(((TextView)findViewById(imageId)).getText().toString());
                                imageURIs[num]=null;
                                RelativeLayout rl=(RelativeLayout)findViewById(buttonId-3);
                                ((ViewGroup)rl.getParent()).removeView(rl);
                            }
                        });

                        TextView imageName=new TextView(AddImagesActivity.this);
                        RelativeLayout.LayoutParams p3=new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                        );
                        p3.addRule(RelativeLayout.LEFT_OF,id2);
                        p3.addRule(RelativeLayout.END_OF,id2);
                        p3.addRule(RelativeLayout.BELOW,id1);
                        imageName.setLayoutParams(p3);
                        int id3=viewCounter++;
                        imageName.setId(id3);
                        imageName.setText(name);
                        relativeLayout.addView(imageName);

                        TextView imageNum=new TextView(AddImagesActivity.this);
                        RelativeLayout.LayoutParams p4=new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                        );
                        p4.addRule(RelativeLayout.RIGHT_OF,id1);
                        imageNum.setLayoutParams(p4);
                        imageNum.setVisibility(View.GONE);
                        imageNum.setText(String.valueOf(i));
                        int id4=viewCounter++;
                        imageNum.setId(id4);
                        relativeLayout.addView(imageNum);

                        mLinearAddImages.addView(relativeLayout);
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


    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AddImagesActivity.this);
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
            Intent intent = new Intent(AddImagesActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_user) {
            Intent intent = new Intent(AddImagesActivity.this, UserActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(AddImagesActivity.this, SearchActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_login) {
            Intent myIntent = new Intent(AddImagesActivity.this,LoginActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_logout){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent intent = new Intent(AddImagesActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Toast.makeText(AddImagesActivity.this,"Uspešno ste se odjavili sa svog naloga.",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(id==R.id.action_add){
            Intent myIntent = new Intent(AddImagesActivity.this,NewAdActivity.class);
            startActivity(myIntent);
        }
        else if(id==R.id.action_home){
            stopService(new Intent(getApplicationContext(),AdCounterPullService.class));
            Intent myIntent = new Intent(AddImagesActivity.this,HomeActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
