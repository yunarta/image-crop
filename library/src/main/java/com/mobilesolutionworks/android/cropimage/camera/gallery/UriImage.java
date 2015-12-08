/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobilesolutionworks.android.cropimage.camera.gallery;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.mobilesolutionworks.android.cropimage.camera.BitmapManager;
import com.mobilesolutionworks.android.cropimage.camera.Util;
import com.mobilesolutionworks.android.graphics.exif.ExifInterface;
import com.mobilesolutionworks.android.graphics.exif.ExifTag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class UriImage implements IImage
{
    private static final String TAG = "UriImage";
    private final Uri             mUri;
    private final IImageList      mContainer;
    private final ContentResolver mContentResolver;
    private       int             mRotation;

    UriImage(IImageList container, ContentResolver cr, Uri uri)
    {
        mContainer = container;
        mContentResolver = cr;
        mUri = uri;
    }

    public int getDegreesRotated()
    {
        return 0;
    }

    public String getDataPath()
    {
        return mUri.getPath();
    }

    private InputStream getInputStream()
    {
        try
        {
            if (mUri.getScheme().equals("file"))
            {
                return new java.io.FileInputStream(mUri.getPath());
            }
            else
            {
                return mContentResolver.openInputStream(mUri);
            }
        }
        catch (FileNotFoundException ex)
        {
            return null;
        }
    }

    private ParcelFileDescriptor getPFD()
    {
        try
        {
            InputStream in = mContentResolver.openInputStream(mUri);
            try
            {
                ExifInterface exifInterface = new ExifInterface();
                exifInterface.readExif(in);

                ExifTag tag        = exifInterface.getTag(ExifInterface.TAG_ORIENTATION);
                int     valueAsInt = tag.getValueAsInt(0);

                mRotation = ExifInterface.getRotationForOrientationValue((short) valueAsInt);
            }
            catch (Exception e)
            {
                mRotation = 0;
            }
            finally
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            if (mUri.getScheme().equals("file"))
            {
                String path = mUri.getPath();
                return ParcelFileDescriptor.open(new File(path),
                        ParcelFileDescriptor.MODE_READ_ONLY);
            }
            else
            {
//                Cursor cursor = mContentResolver.query(mUri, null, null, null, null);
//                if (Arrays.asList(cursor.getColumnNames()).indexOf("mime_type") != -1) {
//                    cursor.moveToNext();
//                    String mimeType = cursor.getString(cursor.getColumnIndex("mime_type"));
//                    if ("image/jpeg".equals(mimeType)) {
//                        InputStream in = mContentResolver.openInputStream(mUri);
//                        try {
//                            ExifInterface exifInterface = new ExifInterface();
//                            exifInterface.readExif(in);
//
//                            ExifTag tag = exifInterface.getTag(ExifInterface.TAG_ORIENTATION);
//                            int valueAsInt = tag.getValueAsInt(0);
//
//                            mRotation = ExifInterface.getRotationForOrientationValue((short) valueAsInt);
//                        } catch (Exception e) {
//
//                        } finally {
//                            try {
//                                in.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }

//                InputStream in = mContentResolver.openInputStream(mUri);
//                try {
//                    ExifInterface exifInterface = new ExifInterface();
//                    exifInterface.readExif(in);
//
//                    ExifTag tag = exifInterface.getTag(ExifInterface.TAG_ORIENTATION);
//                    int valueAsInt = tag.getValueAsInt(0);
//
//                    mRotation = ExifInterface.getRotationForOrientationValue((short) valueAsInt);
//                } catch (Exception e) {
//
//                } finally {
//                    try {
//                        in.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }

                return mContentResolver.openFileDescriptor(mUri, "r");
            }
        }
        catch (FileNotFoundException ex)
        {
            return null;
        }
    }

    public Bitmap fullSizeBitmap(int minSideLength, int maxNumberOfPixels)
    {
        return fullSizeBitmap(minSideLength, maxNumberOfPixels,
                IImage.ROTATE_AS_NEEDED, IImage.NO_NATIVE);
    }

    public Bitmap fullSizeBitmap(int minSideLength, int maxNumberOfPixels,
                                 boolean rotateAsNeeded)
    {
        return fullSizeBitmap(minSideLength, maxNumberOfPixels,
                rotateAsNeeded, IImage.NO_NATIVE);
    }

    public Bitmap fullSizeBitmap(int minSideLength, int maxNumberOfPixels,
                                 boolean rotateAsNeeded, boolean useNative)
    {
        try
        {
            ParcelFileDescriptor pfdInput = getPFD();
            Bitmap b = Util.makeBitmap(minSideLength, maxNumberOfPixels,
                    pfdInput, useNative);
            if (mRotation != 0)
            {
                b = Util.rotate(b, mRotation);
            }
            return b;
        }
        catch (Exception ex)
        {
            Log.e(TAG, "got exception decoding bitmap ", ex);
            return null;
        }
    }

    public Uri fullSizeImageUri()
    {
        return mUri;
    }

    public InputStream fullSizeImageData()
    {
        return getInputStream();
    }

    public Bitmap miniThumbBitmap()
    {
        return thumbBitmap(IImage.ROTATE_AS_NEEDED);
    }

    public String getTitle()
    {
        return mUri.toString();
    }

    public Bitmap thumbBitmap(boolean rotateAsNeeded)
    {
        return fullSizeBitmap(THUMBNAIL_TARGET_SIZE, THUMBNAIL_MAX_NUM_PIXELS,
                rotateAsNeeded);
    }

    private BitmapFactory.Options snifBitmapOptions()
    {
        ParcelFileDescriptor input = getPFD();
        if (input == null) return null;
        try
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapManager.instance().decodeFileDescriptor(
                    input.getFileDescriptor(), options);
            return options;
        }
        finally
        {
            Util.closeSilently(input);
        }
    }

    public String getMimeType()
    {
        BitmapFactory.Options options = snifBitmapOptions();
        return (options != null && options.outMimeType != null)
                ? options.outMimeType
                : "";
    }

    public int getHeight()
    {
        BitmapFactory.Options options = snifBitmapOptions();
        return (options != null) ? options.outHeight : 0;
    }

    public int getWidth()
    {
        BitmapFactory.Options options = snifBitmapOptions();
        return (options != null) ? options.outWidth : 0;
    }

    public IImageList getContainer()
    {
        return mContainer;
    }

    public long getDateTaken()
    {
        return 0;
    }

    public boolean isReadonly()
    {
        return true;
    }

    public boolean isDrm()
    {
        return false;
    }

    public boolean rotateImageBy(int degrees)
    {
        return false;
    }
}
