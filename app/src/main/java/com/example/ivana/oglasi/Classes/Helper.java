package com.example.ivana.oglasi.Classes;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.renderscript.ScriptGroup;
import android.view.ContextMenu;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.TransactionalTask;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.support.Base64;
import com.couchbase.lite.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ivana on 4/4/2017.
 */

public class Helper {

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public static boolean deleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        return fileOrDirectory.delete();
    }

    public static boolean resolveAdConflicts(String docId){
        final Document doc= DatabaseInstance.getInstance().database.getDocument(docId);
        boolean result=false;
        try{
            final List<SavedRevision> conflicts = doc.getConflictingRevisions();
            if (conflicts.size() > 1) {
                DatabaseInstance.getInstance().database.runInTransaction(new TransactionalTask() {
                    @Override
                    public boolean run() {
                        try {
                            Map<String, Object> mergedProps=new HashMap<String, Object>();
                            mergedProps.putAll(conflicts.get(0).getProperties());
                            int updatedAtMerged=(int)mergedProps.get("updated_at");

                            ArrayList<Map<String, Object>> mergedComments=(ArrayList<Map<String, Object>>)mergedProps.get("comments");
                            boolean reportFlag=(boolean)mergedProps.get("report_flag");
                            int reportCount=(int)mergedProps.get("report_count");
                            boolean isDeleted;
                            if(mergedProps.containsKey("deleted")){
                                isDeleted=(boolean)mergedProps.get("deleted");
                            }
                            else{
                                isDeleted=false;
                            }
                            for(int i=1;i<conflicts.size();i++){
                                Map<String, Object> revProps=conflicts.get(i).getProperties();
                                ArrayList<Map<String, Object>> revAd=(ArrayList<Map<String, Object>>)revProps.get("comments");
                                for(int j=0;j<revAd.size();j++){
                                    Map<String, Object> comment=revAd.get(j);
                                    if(!mergedComments.contains(comment)){
                                        mergedComments.add(comment);
                                    }
                                }

                                int updatedAtRev=(int)revProps.get("updated_at");
                                if(updatedAtRev>updatedAtMerged){
                                    mergedProps.put("updated_at",updatedAtRev);
                                    mergedProps.put("title",revProps.get("title"));
                                    mergedProps.put("text",revProps.get("text"));
                                    mergedProps.put("filters",revProps.get("filters"));
                                    mergedProps.put("cat1",revProps.get("cat1"));
                                    mergedProps.put("cat2",revProps.get("cat2"));
                                    mergedProps.put("images",revProps.get("images"));
                                }

                                if(((boolean)revProps.get("report_flag"))==true)
                                    reportFlag=true;

                                if(((int)revProps.get("report_count"))>reportCount)
                                    reportCount=(int)revProps.get("report_count");

                                if(revProps.containsKey("deleted")==true)
                                    isDeleted=(boolean)revProps.get("deleted");
                            }
                            mergedProps.put("comments",mergedComments);
                            mergedProps.put("report_flag",reportFlag);
                            mergedProps.put("deleted",isDeleted);

                            SavedRevision current = doc.getCurrentRevision();
                            for (SavedRevision rev : conflicts) {
                                UnsavedRevision newRev = rev.createRevision();
                                if (rev.getId().equals(current.getId())) {
                                    newRev.setProperties(mergedProps);
                                } else {
                                    newRev.setIsDeletion(true);
                                }
                                newRev.save(true);
                            }
                        } catch (CouchbaseLiteException e) {
                            return false;
                        }
                        return true;
                    }
                });
                result=conflicts.size()==1;
            }
        }
        catch(CouchbaseLiteException e){
            Log.e("Oglasi",e.getMessage());
        }
        return result;
    }

    public static boolean resolveAdCounterConflicts(){
        final Document docCounter= DatabaseInstance.getInstance().database.getDocument("ads_counter");
        final Document docDeleted= DatabaseInstance.getInstance().database.getDocument("ads_deleted");
        boolean result=false;
        try{
            final List<SavedRevision> conflictsCounter = docCounter.getConflictingRevisions();
            final List<SavedRevision> conflictsDeleted = docDeleted.getConflictingRevisions();
            if (conflictsCounter.size() > 1 || conflictsDeleted.size() > 1) {
                // There is more than one current revision, thus a conflict!
                DatabaseInstance.getInstance().database.runInTransaction(new TransactionalTask() {
                    @Override
                    public boolean run() {
                        try {
                            Map<String, Object> mergedPropsCounter=new HashMap<String, Object>();
                            mergedPropsCounter.putAll(conflictsCounter.get(0).getProperties());

                            Map<String, Object> mergedPropsDeleted=new HashMap<String, Object>();
                            mergedPropsDeleted.putAll(conflictsDeleted.get(0).getProperties());

                            List<String> mergedRevCounterValue=(List<String>)mergedPropsCounter.get("ids");
                            List<String> mergedRevDeletedValue=(List<String>)mergedPropsDeleted.get("ids");

                            for(int i=1;i<conflictsCounter.size();i++){
                                Map<String, Object> revProps=conflictsCounter.get(i).getProperties();
                                List<String> revCounterValue=(List<String>)revProps.get("ids");
                                for(int j=0;j<revCounterValue.size();j++){
                                    if(!mergedRevCounterValue.contains(revCounterValue.get(j))){
                                        mergedRevCounterValue.add(revCounterValue.get(j));
                                    }
                                }
                            }
                            for(int i=1;i<conflictsDeleted.size();i++){
                                Map<String, Object> revProps=conflictsDeleted.get(i).getProperties();
                                List<String> revDeletedValue=(List<String>)revProps.get("ids");
                                for(int j=0;j<revDeletedValue.size();j++){
                                    if(!mergedRevDeletedValue.contains(revDeletedValue.get(j))){
                                        mergedRevDeletedValue.add(revDeletedValue.get(j));
                                    }
                                }
                            }
                            mergedPropsCounter.put("ids",mergedRevCounterValue);
                            mergedPropsDeleted.put("ids",mergedRevDeletedValue);

                            // Delete the conflicting revisions to get rid of the conflict:
                            SavedRevision currentCounter = docCounter.getCurrentRevision();
                            for (SavedRevision rev : conflictsCounter) {
                                UnsavedRevision newRev = rev.createRevision();
                                if (rev.getId().equals(currentCounter.getId())) {
                                    newRev.setProperties(mergedPropsCounter);
                                } else {
                                    newRev.setIsDeletion(true);
                                }
                                // saveAllowingConflict allows 'rev' to be updated even if it
                                // is not the document's current revision.
                                newRev.save(true);
                            }

                            SavedRevision currentDeleted = docDeleted.getCurrentRevision();
                            for (SavedRevision rev : conflictsDeleted) {
                                UnsavedRevision newRev = rev.createRevision();
                                if (rev.getId().equals(currentDeleted.getId())) {
                                    newRev.setProperties(mergedPropsDeleted);
                                } else {
                                    newRev.setIsDeletion(true);
                                }
                                // saveAllowingConflict allows 'rev' to be updated even if it
                                // is not the document's current revision.
                                newRev.save(true);
                            }
                        } catch (CouchbaseLiteException e) {
                            return false;
                        }
                        return true;
                    }
                });
                result=conflictsCounter.size()==1 && conflictsDeleted.size() == 1;
            }
        }
        catch(CouchbaseLiteException e){
            Log.e("Oglasi",e.getMessage());
        }
        return result;
    }

    public static boolean resolveUsernamesListConflicts(){
        final Document doc= DatabaseInstance.getInstance().database.getExistingDocument("usernames");
        boolean result=false;
        try{
            final List<SavedRevision> conflicts = doc.getConflictingRevisions();
            if (conflicts.size() > 1) {
                // There is more than one current revision, thus a conflict!
                DatabaseInstance.getInstance().database.runInTransaction(new TransactionalTask() {
                    @Override
                    public boolean run() {
                        try {
                            // Come up with a merged/resolved document in some way that's
                            // appropriate for the app. You could even just pick the body of
                            // one of the revisions.
                            Map<String, Object> mergedProps=new HashMap<String, Object>();
                            mergedProps.putAll(conflicts.get(0).getProperties());

                            ArrayList<String> mergedUsernames=(ArrayList<String>)mergedProps.get("ids");

                            for(int i=1;i<conflicts.size();i++){
                                Map<String, Object> revProps=conflicts.get(i).getProperties();
                                ArrayList<String> revUsernames=(ArrayList<String>)revProps.get("ids");
                                for(int j=0;j<revUsernames.size();j++){
                                    if(!mergedUsernames.contains(revUsernames.get(j))){
                                        mergedUsernames.add(revUsernames.get(j));
                                    }
                                }
                            }

                            // Delete the conflicting revisions to get rid of the conflict:
                            SavedRevision current = doc.getCurrentRevision();
                            for (SavedRevision rev : conflicts) {
                                UnsavedRevision newRev = rev.createRevision();
                                if (rev.getId().equals(current.getId())) {
                                    newRev.setProperties(mergedProps);
                                } else {
                                    newRev.setIsDeletion(true);
                                }
                                // saveAllowingConflict allows 'rev' to be updated even if it
                                // is not the document's current revision.
                                newRev.save(true);
                            }
                        } catch (CouchbaseLiteException e) {
                            return false;
                        }
                        return true;
                    }
                });
                result=conflicts.size()==1;
            }
        }
        catch(CouchbaseLiteException e){
            Log.e("Oglasi",e.getMessage());
        }
        return result;
    }

    public static boolean resolveDeletedUsersListConflicts(){
        final Document doc= DatabaseInstance.getInstance().database.getExistingDocument("users_deleted");
        boolean result=false;
        try{
            final List<SavedRevision> conflicts = doc.getConflictingRevisions();
            if (conflicts.size() > 1) {
                // There is more than one current revision, thus a conflict!
                DatabaseInstance.getInstance().database.runInTransaction(new TransactionalTask() {
                    @Override
                    public boolean run() {
                        try {
                            // Come up with a merged/resolved document in some way that's
                            // appropriate for the app. You could even just pick the body of
                            // one of the revisions.
                            Map<String, Object> mergedProps=new HashMap<String, Object>();
                            mergedProps.putAll(conflicts.get(0).getProperties());

                            ArrayList<String> mergedUsernames=(ArrayList<String>)mergedProps.get("usernames");

                            for(int i=1;i<conflicts.size();i++){
                                Map<String, Object> revProps=conflicts.get(i).getProperties();
                                ArrayList<String> revUsernames=(ArrayList<String>)revProps.get("usernames");
                                for(int j=0;j<revUsernames.size();j++){
                                    if(!mergedUsernames.contains(revUsernames.get(j))){
                                        mergedUsernames.add(revUsernames.get(j));
                                    }
                                }
                            }

                            // Delete the conflicting revisions to get rid of the conflict:
                            SavedRevision current = doc.getCurrentRevision();
                            for (SavedRevision rev : conflicts) {
                                UnsavedRevision newRev = rev.createRevision();
                                if (rev.getId().equals(current.getId())) {
                                    newRev.setProperties(mergedProps);
                                } else {
                                    newRev.setIsDeletion(true);
                                }
                                // saveAllowingConflict allows 'rev' to be updated even if it
                                // is not the document's current revision.
                                newRev.save(true);
                            }
                        } catch (CouchbaseLiteException e) {
                            return false;
                        }
                        return true;
                    }
                });
                result=conflicts.size()==1;
            }
        }
        catch(CouchbaseLiteException e){
            Log.e("Oglasi",e.getMessage());
        }
        return result;
    }

    public static boolean resolveUserConflicts(String userId){
        final Document doc= DatabaseInstance.getInstance().database.getDocument(userId);
        boolean result=false;
        try{
            final List<SavedRevision> conflicts = doc.getConflictingRevisions();
            if (conflicts.size() > 1) {
                // There is more than one current revision, thus a conflict!
                DatabaseInstance.getInstance().database.runInTransaction(new TransactionalTask() {
                    @Override
                    public boolean run() {
                        try {
                            // Come up with a merged/resolved document in some way that's
                            // appropriate for the app. You could even just pick the body of
                            // one of the revisions.
                            Map<String, Object> mergedProps=new HashMap<String, Object>();
                            mergedProps.putAll(conflicts.get(0).getProperties());

                            ArrayList<String> mergedUserAds=(ArrayList<String>)mergedProps.get("ads");

                            for(int i=1;i<conflicts.size();i++){
                                Map<String, Object> revProps=conflicts.get(i).getProperties();
                                ArrayList<String> revUserAds=(ArrayList<String>)revProps.get("ads");
                                for(int j=0;j<revUserAds.size();j++){
                                    if(!mergedUserAds.contains(revUserAds.get(j))){
                                        mergedUserAds.add(revUserAds.get(j));
                                    }
                                }
                            }

                            // Delete the conflicting revisions to get rid of the conflict:
                            SavedRevision current = doc.getCurrentRevision();
                            for (SavedRevision rev : conflicts) {
                                UnsavedRevision newRev = rev.createRevision();
                                if (rev.getId().equals(current.getId())) {
                                    newRev.setProperties(mergedProps);
                                } else {
                                    newRev.setIsDeletion(true);
                                }
                                // saveAllowingConflict allows 'rev' to be updated even if it
                                // is not the document's current revision.
                                newRev.save(true);
                            }
                        } catch (CouchbaseLiteException e) {
                            return false;
                        }
                        return true;
                    }
                });
                result=conflicts.size()==1;
            }
        }
        catch(CouchbaseLiteException e){
            Log.e("Oglasi",e.getMessage());
        }
        return result;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) >= reqHeight
                        && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromAttachment(Attachment attachment, Rect outPadding, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeResource(res, resId, options);
        try{
            InputStream is=attachment.getContent();
            BitmapFactory.decodeStream(is,outPadding,options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            is.close();
            is=attachment.getContent();
            Bitmap bitmap=BitmapFactory.decodeStream(is,outPadding,options);
            is.close();
            return bitmap;
        } catch (CouchbaseLiteException e){
            Log.e("Oglasi",e.getMessage());
        } catch (IOException e){

        }
        return null;
    }

    public static Bitmap decodeSampledBitmapFromUri(Uri uri, Context context, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeResource(res, resId, options);
        try{
            InputStream is = context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(is,null,options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            is.close();
            is = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap=BitmapFactory.decodeStream(is,null,options);
            is.close();
            return bitmap;
        } catch (IOException e){
            Log.e("Oglasi",e.getMessage());
        }
        return null;
    }

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
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
}
