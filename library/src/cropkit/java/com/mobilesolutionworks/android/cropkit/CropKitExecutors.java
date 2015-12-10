package com.mobilesolutionworks.android.cropkit;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * Created by yunarta on 10/12/15.
 */
public class CropKitExecutors implements Executor
{
    static CropKitExecutors sExecutors = new CropKitExecutors();

    public static CropKitExecutors get()
    {
        return sExecutors;
    }

    Handler mHandler;

    public CropKitExecutors()
    {
        mHandler = new Handler(Looper.myLooper());
    }

    public CropKitExecutors(Handler handler)
    {
        mHandler = handler;
    }

    @Override
    public void execute(Runnable runnable)
    {
        mHandler.post(runnable);
    }
}
