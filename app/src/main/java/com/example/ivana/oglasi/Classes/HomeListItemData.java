package com.example.ivana.oglasi.Classes;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
 * Created by Ivana on 2/5/2017.
 */

public class HomeListItemData {

    private String adTitle;
    private String adText;
    private Bitmap adImage;
    private String adId;

    public HomeListItemData(String adTitle, String adText, Bitmap adImage, String id){
        this.setAdTitle(adTitle);
        this.setAdText(adText);
        this.setAdImage(adImage);
        this.adId=id;
    }

    public String getAdTitle() {
        return adTitle;
    }

    public void setAdTitle(String adTitle) {
        this.adTitle = adTitle;
    }

    public String getAdText() {
        return adText;
    }

    public void setAdText(String adText) {
        this.adText = adText;
    }

    public Bitmap getAdImage() {
        return adImage;
    }

    public void setAdImage(Bitmap adImage) {
        this.adImage=adImage;
    }

    public String getAdId() { return adId; }

    public void setAdId(String adId) { this.adId=adId; }
}