package com.mobilesolutionworks.android.cropkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import com.mobilesolutionworks.android.graphics.exif.ExifInterface;

import java.io.IOException;

/**
 * Created by yunarta on 23/12/15.
 */
public class CropKitUtil
{
    public static class HDCropInfo
    {
        /**
         * User additional rotation applied to original bitmap
         */
        public int rotation;

        /**
         * Rectangle of the full bitmap size in scaled mode.
         * <p/>
         * If not null, mean crop rect is a scaled rect
         */
        public RectF scaledRect;

        /**
         * Crop rectangle
         */
        public RectF cropRect;
    }

    public static Bitmap HDCrop(Context context, String path, HDCropInfo cropInfo, int maxLength, boolean scaleDownOnly) throws IOException
    {
        float maxScreenLength;
        int   orientationRotation;

        {
            // get rotation of the jpeg file
            ExifInterface exif = new ExifInterface();
            exif.readExif(path);

            Integer value = exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
            orientationRotation = ExifInterface.getRotationForOrientationValue(
                    value != null ? (short) value.intValue() : ExifInterface.Orientation.TOP_LEFT
            );

            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            maxScreenLength = Math.max(metrics.widthPixels, metrics.heightPixels);
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(path, opts);

        Matrix matrix = new Matrix();

        // firstly, we need to transform the cropRect and scaledRect
        // back to match the original bitmap dimension and orientation + rotation
        RectF scaledRect = new RectF(cropInfo.scaledRect);
        RectF cropRect   = new RectF(cropInfo.cropRect);

        // inverse transform orientation and rotation
        matrix.setTranslate(-scaledRect.width() / 2, -scaledRect.height() / 2);
        matrix.mapRect(scaledRect);
        matrix.mapRect(cropRect);

        matrix.setRotate(-orientationRotation);
        matrix.postRotate(-cropInfo.rotation);

        matrix.mapRect(scaledRect);
        matrix.mapRect(cropRect);

        matrix.setTranslate(scaledRect.width() / 2, scaledRect.height() / 2);
        matrix.mapRect(scaledRect);
        matrix.mapRect(cropRect);

        // inverse scale
        matrix.setScale(opts.outWidth / scaledRect.width(), opts.outHeight / scaledRect.height());
        matrix.mapRect(scaledRect);
        matrix.mapRect(cropRect);

        // get the required sample rate needed to create this crop
        opts.inSampleSize = (int) (maxLength / Math.max(cropRect.width(), cropRect.height()));
        opts.inJustDecodeBounds = false;
        opts.inMutable = true;

        // decode the bitmap according to the sample rate
        int    outWidth = opts.outWidth;
        Bitmap bitmap   = BitmapFactory.decodeFile(path, opts);

        float scaleDown = (float) bitmap.getWidth() / outWidth;

        matrix.setScale(scaleDown, scaleDown);
        matrix.mapRect(cropRect);

        scaleDown = maxLength / Math.max(cropRect.width(), cropRect.height());

        matrix.setRotate(orientationRotation);
        matrix.postRotate(cropInfo.rotation);

        if (scaleDown < 0 || !scaleDownOnly)
        {
            matrix.postScale(scaleDown, scaleDown);
        }

        Bitmap rotate = Bitmap.createBitmap(bitmap,
                (int) cropRect.left, (int) cropRect.top,
                (int) cropRect.width(), (int) cropRect.height(),
                matrix, true);
        if (rotate != bitmap) bitmap.recycle();

        return rotate;
    }
}
