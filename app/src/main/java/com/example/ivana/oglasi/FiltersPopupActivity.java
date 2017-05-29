package com.example.ivana.oglasi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by Ivana on 4/11/2017.
 */

public class FiltersPopupActivity extends Activity {

    Spinner mCat1;
    Spinner mCat2;
    Button mApplyFilters;
    TextView mRestart;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filters_popup);

        DisplayMetrics dm=new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width=dm.widthPixels;
        int height=dm.heightPixels;
        getWindow().setLayout((int)(width*0.8),(int)(height*0.45));

        mCat1=(Spinner)findViewById(R.id.spinner_searchCat1);
        mCat2=(Spinner)findViewById(R.id.spinner_searchCat2);
        mApplyFilters=(Button)findViewById(R.id.button_applyFilters);
        mRestart=(TextView)findViewById(R.id.textView_restartSearch);

        String[] cat1Data=getResources().getStringArray(R.array.cat_1);
        for(int i=0;i<cat1Data.length;i++){
            if(cat1Data[i].equals(getIntent().getStringExtra("cat1"))){
                mCat1.setSelection(i);
                break;
            }
        }

        mApplyFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(FiltersPopupActivity.this,SearchActivity.class);
                intent.putExtra("cat1", mCat1.getSelectedItem().toString());
                intent.putExtra("cat2", mCat2.getSelectedItem().toString());
                setResult(RESULT_OK,intent);
                finish();
                startActivity(intent);
            }
        });

        mRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(FiltersPopupActivity.this,SearchActivity.class);
                intent.putExtra("cat1", "--");
                intent.putExtra("cat2", "--");
                setResult(RESULT_OK,intent);
                finish();
                startActivity(intent);
            }
        });

        mCat1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(spinner2InitialSet){
                    String[] cat2Data=getResources().getStringArray(LOOKUP_TABLE[position]);
                    ArrayAdapter<String> adapter2=new ArrayAdapter<String>(FiltersPopupActivity.this,android.R.layout.simple_spinner_dropdown_item,cat2Data);
                    mCat2.setAdapter(adapter2);
                    for(int j=0;j<cat2Data.length;j++){
                        if(cat2Data[j].equals(getIntent().getStringExtra("cat2"))){
                            mCat2.setSelection(j);
                            break;
                        }
                    }
                    spinner2InitialSet=false;
                }
                else{
                    String[] cat2Data=getResources().getStringArray(LOOKUP_TABLE[position]);
                    ArrayAdapter<String> adapter=new ArrayAdapter<String>(FiltersPopupActivity.this,android.R.layout.simple_spinner_dropdown_item,cat2Data);
                    mCat2.setAdapter(adapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
