package com.mobilesolutionworks.android.cropkit;

import android.os.Bundle;

/**
 * Created by yunarta on 14/12/15.
 */
public class CropKitBuilder
{
    public int aspectX;

    public int aspectY;

    public boolean circleCrop;

    public boolean detectFace;

    public Bundle create()
    {
        Bundle bundle = new Bundle();
        bundle.putInt("aspectX", aspectX);
        bundle.putInt("aspectY", aspectY);
        bundle.putBoolean("circleCrop", circleCrop);
        bundle.putBoolean("faceDetection", detectFace);

        return bundle;
    }
}
