package com.mobilesolutionworks.android.cropkit.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.mobilesolutionworks.android.cropkit.CropImageView;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.three);

        CropImageView imageView = (CropImageView) findViewById(R.id.cropkit);
        imageView.setImageBitmapResetBase(bitmap, true);
    }
}
