package com.example.ivana.oglasi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.Document;
import com.couchbase.lite.Revision;
import com.example.ivana.oglasi.Classes.DatabaseInstance;
import com.example.ivana.oglasi.Classes.Helper;

import java.io.File;
import java.util.ArrayList;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

//        Document adDoc= DatabaseInstance.getInstance().database.getExistingDocument(getIntent().getExtras().getString("ad_id"));
//        Revision rev=adDoc.getCurrentRevision();
//        Attachment att=rev.getAttachment(getIntent().getExtras().getString("attachment_name"));
//        Bitmap bm= Helper.decodeSampledBitmapFromAttachment(att,null,400,400);

        String path=getIntent().getStringExtra("file_name");
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), path);
        Uri imgUri=Uri.fromFile(file);
        Bitmap bm= Helper.decodeSampledBitmapFromUri(imgUri,getApplicationContext(),1000,1000);

//        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());

        ImageView imageView = (ImageView) findViewById(R.id.imageView_image);
        imageView.setImageBitmap(bm);
    }
}
