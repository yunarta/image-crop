package com.mobilesolutionworks.android.cropkit.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.mobilesolutionworks.android.cropkit.CropKitBuilder;
import com.mobilesolutionworks.android.cropkit.CropKitFragment;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.three);
//
//        CropImageView imageView = (CropImageView) findViewById(R.id.cropkit);
//        imageView.setImageBitmapResetBase(bitmap, true);

        if (savedInstanceState == null)
        {
            CropKitBuilder ckb = new CropKitBuilder();
            ckb.aspectX = 3;
            ckb.aspectY = 4;
            ckb.detectFace = true;

            CropKitFragment fragment = new MyCropKitFragment();
            fragment.setArguments(ckb.create());

            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
        }
    }
}
