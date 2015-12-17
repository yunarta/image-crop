package com.mobilesolutionworks.android.cropkit;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mobilesolutionworks.android.cropimage.R;

/**
 * Created by yunarta on 8/12/15.
 */
public class CropKitFragment extends Fragment
{
    private CropImageView mCropKit;

    private boolean mCircleCrop;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mCircleCrop = args.getBoolean("circleCrop", false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_basic_cropkit, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        mCropKit = (CropImageView) view.findViewById(R.id.cropkit);

        Bundle args = getArguments();

        mCropKit.setAspect(args.getInt("aspectX", 1), args.getInt("aspectY", 1));
        mCropKit.setCircleCrop(args.getBoolean("circleCrop", false));
        mCropKit.setFaceDetection(args.getBoolean("faceDetection", false));
    }

    public CropImageView getCropKit()
    {
        return mCropKit;
    }

    public Bitmap getCroppedBitmap()
    {
        Bitmap croppedImage;

//        // If the output is required to a specific size, create an new image
//        // with the cropped image in the center and the extra space filled.
//        if (mOutputX != 0 && mOutputY != 0 && !mScale)
//        {
//            // Don't scale the image but instead fill it so it's the
//            // required dimension
//            croppedImage = Bitmap.createBitmap(mOutputX, mOutputY, Bitmap.Config.RGB_565);
//            Canvas canvas = new Canvas(croppedImage);
//
//            Rect srcRect = mCrop.getCropRect();
//            Rect dstRect = new Rect(0, 0, mOutputX, mOutputY);
//
//            int dx = (srcRect.width() - dstRect.width()) / 2;
//            int dy = (srcRect.height() - dstRect.height()) / 2;
//
//            // If the srcRect is too big, use the center part of it.
//            srcRect.inset(Math.max(0, dx), Math.max(0, dy));
//
//            // If the dstRect is too big, use the center part of it.
//            dstRect.inset(Math.max(0, -dx), Math.max(0, -dy));
//
//            // Draw the cropped bitmap in the center
//            canvas.drawBitmap(mBitmap, srcRect, dstRect, null);
//
//            // Release bitmap memory as soon as possible
//            mImageView.clear();
//            mBitmap.recycle();
//        }
//        else

        {
            CropImageView kit        = getCropKit();
            Bitmap        baseBitmap = kit.getBaseBitmap();

            Rect r = kit.getSelectedCropArea();

            int width  = r.width();
            int height = r.height();

            // If we are circle cropping, we want alpha channel, which is the
            // third param here.
            croppedImage = Bitmap.createBitmap(width, height, mCircleCrop ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);

            Canvas canvas = new Canvas(croppedImage);

            if (mCircleCrop)
            {
                final int   color = 0xffff0000;
                final Paint paint = new Paint();
                final Rect  rect  = new Rect(0, 0, croppedImage.getWidth(), croppedImage.getHeight());
                final RectF rectF = new RectF(rect);

                paint.setAntiAlias(true);
                paint.setDither(true);
                paint.setFilterBitmap(true);
                canvas.drawARGB(0, 0, 0, 0);
                paint.setColor(color);
                canvas.drawOval(rectF, paint);

                paint.setColor(Color.BLUE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth((float) 4);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                canvas.drawBitmap(baseBitmap, r, rect, paint);
            }
            else
            {
                Rect dstRect = new Rect(0, 0, width, height);
                canvas.drawBitmap(baseBitmap, r, dstRect, null);
            }

//            kit.setImageBitmapResetBase(croppedImage, true);
//            kit.center(true, true);

//            // Release bitmap memory as soon as possible
//            mImageView.clear();
//            mBitmap.recycle();
//
//            // If the required dimension is specified, scale the image.
//            if (mOutputX != 0 && mOutputY != 0 && mScale)
//            {
//                croppedImage = Util.transform(new Matrix(), croppedImage,
//                        mOutputX, mOutputY, mScaleUp, Util.RECYCLE_INPUT);
//            }
        }

        return croppedImage;
    }
}
