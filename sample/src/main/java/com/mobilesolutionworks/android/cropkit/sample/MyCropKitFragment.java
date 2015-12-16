package com.mobilesolutionworks.android.cropkit.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mobilesolutionworks.android.cropkit.CropKitFragment;

/**
 * Created by yunarta on 14/12/15.
 */
public class MyCropKitFragment extends CropKitFragment
{
    Handler mHandler = new Handler();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_my_cropkit, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        OnClickListenerImpl l = new OnClickListenerImpl();

        view.findViewById(R.id.btn_save).setOnClickListener(l);
        view.findViewById(R.id.btn_retake).setOnClickListener(l);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        mHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.three);
                getCropKit().setImageBitmapResetBase(bitmap, true);
            }
        }, 2000);
    }

    private class OnClickListenerImpl implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.btn_save:
                {
                    getCroppedBitmap();


                    break;
                }

                case R.id.btn_retake:
                {
                    break;
                }
            }
        }
    }
}
