package com.mobilesolutionworks.android.cropkit.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
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
                DisplayMetrics metrics         = getResources().getDisplayMetrics();
                int            maxScreenLength = Math.max(metrics.widthPixels, metrics.heightPixels);

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;

                BitmapFactory.decodeResource(getResources(), R.drawable.halcyon1, opts);
                int maxLength = Math.max(opts.outWidth, opts.outHeight);

                opts.inSampleSize = Math.max(1, maxLength / maxScreenLength * 2);
                opts.inJustDecodeBounds = false;
                opts.inDensity = metrics.densityDpi;
                opts.inScreenDensity = metrics.densityDpi;
                opts.inTargetDensity = metrics.densityDpi;

                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.halcyon1, opts);

                Matrix matrix = new Matrix();
//                matrix.postRotate(value);

                Bitmap rotate = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotate != bitmap) bitmap.recycle();

                getCropKit().setImageBitmapResetBase(rotate, true);
            }
        }, 10);
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
