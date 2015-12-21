package com.mobilesolutionworks.android.cropkit;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.FaceDetector;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import bolts.Continuation;
import bolts.Task;
import bolts.TaskCompletionSource;
import com.mobilesolutionworks.android.cropimage.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Realization of old CropKit functionality, now most functionality managed internally by the widget.
 * <p/>
 * Created by yunarta on 7/7/14.
 */
public class CropImageView extends ImageViewTouchBase
{
    // start configuration properties //
    private final LayerDrawable mHighlight;

    private boolean mDoFaceDetection;

    private boolean mCircleCrop;

    private int mAspectX;

    private int mAspectY;

    private Bitmap mImageBitmapResetBase;

    // gesture properties
    private float mLastX;

    private float mLastY;

    private HighlightView mMotionHighlightView = null;

    private int mMotionEdge;

    // class properties

    private List<HighlightView> mHighlightViews = new ArrayList<>();

    private HighlightView mCrop;

    // operation properties

    private TaskCompletionSource<Void> mFaceDetectionTask;

    private boolean mWaitingToPick;

    private Rect mUserRect;

    public CropImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        // load layer list for HighlightView
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CropImageView, R.attr.cropKitStyle, R.style.CropKit);

        Drawable dHighlight = a.getDrawable(R.styleable.CropImageView_cropKitHighlight);
        if (!(dHighlight instanceof LayerDrawable))
        {
            throw new IllegalStateException("cropKitHightlight must be a layer-list");
        }

        mHighlight = (LayerDrawable) dHighlight;
        int[] idCheck = {R.id.cropkit_highlight_diagonal, R.id.cropkit_highlight_horizontal, R.id.cropkit_highlight_vertical};

        // validate layer list member
        for (int id : idCheck)
        {
            if (mHighlight.findDrawableByLayerId(id) == null)
            {
                throw new IllegalStateException("@id/" + getResources().getResourceEntryName(id) + " is not included in cropKitHightlight layer-list");
            }
        }

        mAspectX = a.getInteger(R.styleable.CropImageView_cropKitAspectX, 0);
        mAspectY = a.getInteger(R.styleable.CropImageView_cropKitAspectY, 0);
        mCircleCrop = a.getBoolean(R.styleable.CropImageView_cropKitCircleCrop, false);
        mDoFaceDetection = a.getBoolean(R.styleable.CropImageView_cropKitDetectFace, false);

        a.recycle();
    }

    public void setAspect(int x, int y)
    {
        mAspectX = x;
        mAspectY = y;
    }

    public void setCircleCrop(boolean enable)
    {
        mCircleCrop = enable;
    }

    public void setFaceDetection(boolean enabled)
    {
        mDoFaceDetection = enabled;
    }

    public void setUserRect(Rect rect)
    {
        if (rect != null)
        {
            mDoFaceDetection = false;
            mUserRect = rect;
        }
    }

    @Override
    public void setImageBitmapResetBase(Bitmap bitmap, boolean resetSupp)
    {
        mImageBitmapResetBase = bitmap;
        super.setImageBitmapResetBase(bitmap, resetSupp);

        setupView();
    }

    public Bitmap getBaseBitmap()
    {
        return mImageBitmapResetBase;
    }

    public Rect getSelectedCropArea()
    {
        return mCrop.getCropRect();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);
        if (mBitmapDisplayed.getBitmap() != null)
        {
            Log.d("/!", "onLayout");
            for (HighlightView hv : mHighlightViews)
            {
                hv.setMatrix(getImageMatrix());
                hv.invalidate();
                if (hv.mIsFocused)
                {
                    centerBasedOnHighlightView(hv);
                }
            }
        }
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        setupView();
    }

    private void setupView()
    {
        if (mImageBitmapResetBase == null) return;

        mHighlightViews = new ArrayList<>();
        if (mDoFaceDetection && mFaceDetectionTask == null)
        {
            mCrop = null;
            mWaitingToPick = true;

            mFaceDetectionTask = new TaskCompletionSource<>();
//            if (getScale() == 1F)
            {
                center(true, true);
            }

            Task.callInBackground(new FaceDetectorTask(this)).onSuccess(new Continuation<List<HighlightView>, Object>()
            {
                @Override
                public Object then(Task<List<HighlightView>> task) throws Exception
                {
                    List<HighlightView> views = task.getResult();

                    mWaitingToPick = views.size() > 1;
                    if (!mWaitingToPick)
                    {
                        if (views.isEmpty())
                        {
                            _add(createDefaultHighlight());
                        }
                        else
                        {
                            HighlightView hv = views.get(0);
                            _add(hv);
                        }
                    }
                    else
                    {
                        for (HighlightView hv : views)
                        {
                            _add(hv);
                        }
                    }

                    invalidate();
                    if (mHighlightViews.size() == 1)
                    {
                        mCrop = mHighlightViews.get(0);
                        mCrop.setFocus(true);
                    }

//                                if (mNumFaces > 1)
//                                {
////                        Toast t = Toast.makeText(CropImage.this, metaData.getInt("imagecrop_multiface_crop_help", R.string.z_imagecrop_multiface_crop_help), Toast.LENGTH_SHORT);
////                        t.show();
//                                }

                    mFaceDetectionTask.trySetResult(null);
//                    mFaceDetectionTask = new TaskCompletionSource<Void>();
                    return null;
                }
            }, new CropKitExecutors(getHandler())).continueWith(new Continuation<Object, Object>()
            {
                @Override
                public Object then(Task<Object> task) throws Exception
                {
                    if (task.isFaulted())
                    {
                        Log.d("/!", "error", task.getError());
                    }
                    return null;
                }
            });
        }
        else
        {
            if (mUserRect != null)
            {
                _add(createHighlight(mUserRect));
            }
            else
            {
                _add(createDefaultHighlight());
            }

            invalidate();
            if (mHighlightViews.size() == 1)
            {
                mCrop = mHighlightViews.get(0);
                mCrop.setFocus(true);
            }
        }
    }

    @Override
    protected void zoomTo(float scale, float centerX, float centerY)
    {
        Log.d("/!", "zoomTo");
        super.zoomTo(scale, centerX, centerY);
        for (HighlightView hv : mHighlightViews)
        {
            hv.setMatrix(getImageMatrix());
            hv.invalidate();
        }
    }

    @Override
    protected void zoomIn()
    {
        Log.d("/!", "zoomIn");
        super.zoomIn();
        for (HighlightView hv : mHighlightViews)
        {
            hv.setMatrix(getImageMatrix());
            hv.invalidate();
        }
    }

    @Override
    protected void zoomOut()
    {
        Log.d("/!", "zoomOut");
        super.zoomOut();
        for (HighlightView hv : mHighlightViews)
        {
            hv.setMatrix(getImageMatrix());
            hv.invalidate();
        }
    }

    @Override
    protected void postTranslate(float deltaX, float deltaY)
    {
        Log.d("/!", "postTranslate");
        super.postTranslate(deltaX, deltaY);
        for (HighlightView hv : mHighlightViews)
        {
            hv.postTranslate(deltaX, deltaY);
            hv.invalidate();
        }
    }

    // According to the event's position, change the focus to the first
    // hitting cropping rectangle.
    private void recomputeFocus(MotionEvent event)
    {
        for (HighlightView hv : mHighlightViews)
        {
            hv.setFocus(false);
            hv.invalidate();
        }

        for (HighlightView hv : mHighlightViews)
        {
            int edge = hv.getHit(event.getX(), event.getY());
            if (edge != HighlightView.GROW_NONE)
            {
                if (!hv.hasFocus())
                {
                    hv.setFocus(true);
                    hv.invalidate();
                }
                break;
            }
        }

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                if (mWaitingToPick)
                {
                    recomputeFocus(event);
                }
                else
                {
                    for (HighlightView hv : mHighlightViews)
                    {
                        int edge = hv.getHit(event.getX(), event.getY());
                        if (edge != HighlightView.GROW_NONE && hv.hasFocus())
                        {
                            mMotionEdge = edge;
                            mMotionHighlightView = hv;
                            mLastX = event.getX();
                            mLastY = event.getY();
                            mMotionHighlightView.setMode(
                                    (edge == HighlightView.MOVE)
                                            ? HighlightView.ModifyMode.Move
                                            : HighlightView.ModifyMode.Grow
                            );
                            break;
                        }
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            {
                if (mWaitingToPick)
                {
                    for (int i = 0; i < mHighlightViews.size(); i++)
                    {
                        HighlightView hv = mHighlightViews.get(i);
                        if (hv.hasFocus())
                        {
                            mCrop = hv;
                            for (int j = 0; j < mHighlightViews.size(); j++)
                            {
                                if (j == i)
                                {
                                    continue;
                                }
                                mHighlightViews.get(j).setHidden(true);
                            }

                            centerBasedOnHighlightView(hv);
                            mWaitingToPick = false;
                            return true;
                        }
                    }
                }
                else if (mMotionHighlightView != null)
                {
                    centerBasedOnHighlightView(mMotionHighlightView);
                    mMotionHighlightView.setMode(
                            HighlightView.ModifyMode.None);
                }
                mMotionHighlightView = null;
                break;
            }

            case MotionEvent.ACTION_MOVE:
            {
                if (mWaitingToPick)
                {
                    recomputeFocus(event);
                }

                else if (mMotionHighlightView != null)
                {
                    mMotionHighlightView.handleMotion(mMotionEdge, event.getX() - mLastX, event.getY() - mLastY);
                    mLastX = event.getX();
                    mLastY = event.getY();

                    // if (true)
                    {
                        // This section of code is optional. It has some user
                        // benefit in that moving the crop rectangle against
                        // the edge of the screen causes scrolling but it means
                        // that the crop rectangle is no longer fixed under
                        // the user's finger.
                        ensureVisible(mMotionHighlightView);
                    }
                }
                break;
            }
        }

        switch (event.getAction())
        {
            case MotionEvent.ACTION_UP:
            {
                center(true, true);
                break;
            }

            case MotionEvent.ACTION_MOVE:
            {
                // if we're not zoomed then there's no point in even allowing
                // the user to move the image around.  This call to center puts
                // it back to the normalized location (with false meaning don't
                // animate).
                if (getScale() == 1F)
                {
                    center(true, true);
                }
                break;
            }
        }

        return true;
    }

    // If the cropping rectangle's size changed significantly, change the
    // view's center and scale according to the cropping rectangle.
    private void centerBasedOnHighlightView(HighlightView hv)
    {
        Rect drawRect = hv.mDrawRect;

        float width  = drawRect.width();
        float height = drawRect.height();

        float thisWidth  = getWidth();
        float thisHeight = getHeight();

        float z1 = thisWidth / width * .5F;
        float z2 = thisHeight / height * .5F;

        float zoom = Math.min(z1, z2);
        zoom = zoom * this.getScale();
        zoom = Math.max(1F, zoom);

        if ((Math.abs(zoom - getScale()) / zoom) > .1)
        {
            float[] coordinates = new float[]{hv.mCropRect.centerX(), hv.mCropRect.centerY()};

            getImageMatrix().mapPoints(coordinates);
            zoomTo(zoom, coordinates[0], coordinates[1], 300F);
        }

        ensureVisible(hv);
    }

    // Pan the displayed image to make sure the cropping rectangle is visible.
    private void ensureVisible(HighlightView hv)
    {
        Rect r = hv.mDrawRect;

        int panDeltaX1 = Math.max(0, this.getLeft() - r.left);
        int panDeltaX2 = Math.min(0, this.getRight() - r.right);

        int panDeltaY1 = Math.max(0, this.getTop() - r.top);
        int panDeltaY2 = Math.min(0, this.getBottom() - r.bottom);

        int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
        int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

        if (panDeltaX != 0 || panDeltaY != 0)
        {
            panBy(panDeltaX, panDeltaY);
        }
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        for (HighlightView hv : mHighlightViews)
        {
            hv.draw(canvas);
        }
    }

    public void add(HighlightView hv)
    {
        mHighlightViews.add(hv);
        invalidate();
    }

    private void _add(HighlightView hv)
    {
        mHighlightViews.add(hv);
    }

    public void clearHighlights()
    {
        mHighlightViews.clear();
    }

    // For each face, we create a HighlightView for it.
    private HighlightView createHighlightForFace(FaceDetector.Face f, float scale)
    {
        PointF midPoint = new PointF();

        int r = ((int) (f.eyesDistance() * scale)) * 2;

        f.getMidPoint(midPoint);

        midPoint.x *= scale;
        midPoint.y *= scale;

        int midX = (int) midPoint.x;
        int midY = (int) midPoint.y;

        int width  = mImageBitmapResetBase.getWidth();
        int height = mImageBitmapResetBase.getHeight();

        Rect imageRect = new Rect(0, 0, width, height);

        RectF faceRect = new RectF(midX, midY, midX, midY);
        faceRect.inset(-r, -r);

        if (faceRect.left < 0)
        {
            faceRect.inset(-faceRect.left, -faceRect.left);
        }

        if (faceRect.top < 0)
        {
            faceRect.inset(-faceRect.top, -faceRect.top);
        }

        if (faceRect.right > imageRect.right)
        {
            faceRect.inset(faceRect.right - imageRect.right, faceRect.right - imageRect.right);
        }

        if (faceRect.bottom > imageRect.bottom)
        {
            faceRect.inset(faceRect.bottom - imageRect.bottom, faceRect.bottom - imageRect.bottom);
        }

        Log.d("/!", "getImageViewMatrix() = " + getImageViewMatrix());

        HighlightView hv = new HighlightView(this, mHighlight);
        hv.setup(getImageViewMatrix(), imageRect, faceRect, mCircleCrop, mAspectX != 0 && mAspectY != 0);

        return hv;
    }

    // Create a default HightlightView if we found no face in the picture.
    private HighlightView createDefaultHighlight()
    {
        int width  = mImageBitmapResetBase.getWidth();
        int height = mImageBitmapResetBase.getHeight();

        Rect imageRect = new Rect(0, 0, width, height);

        // make the default size about 4/5 of the width or height
        int cropWidth = Math.min(width, height) * 4 / 5;

        //noinspection SuspiciousNameCombination
        int cropHeight = cropWidth;

        if (mAspectX != 0 && mAspectY != 0)
        {
            if (mAspectX > mAspectY)
            {
                cropHeight = cropWidth * mAspectY / mAspectX;
            }
            else
            {
                cropWidth = cropHeight * mAspectX / mAspectY;
            }
        }

        int x = (width - cropWidth) / 2;
        int y = (height - cropHeight) / 2;

        RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);

        Log.d("/!", "getImageViewMatrix() = " + getImageViewMatrix());

        HighlightView hv = new HighlightView(this, mHighlight);
        hv.setup(getImageViewMatrix(), imageRect, cropRect, mCircleCrop, mAspectX != 0 && mAspectY != 0);

        return hv;
    }

    // Create a default HightlightView if we found no face in the picture.
    private HighlightView createHighlight(Rect rect)
    {
        int width  = mImageBitmapResetBase.getWidth();
        int height = mImageBitmapResetBase.getHeight();

        Rect imageRect = new Rect(0, 0, width, height);

//
//        // make the default size about 4/5 of the width or height
//        int cropWidth = Math.min(width, height) * 4 / 5;
//
//        //noinspection SuspiciousNameCombination
//        int cropHeight = cropWidth;
//
//        if (mAspectX != 0 && mAspectY != 0)
//        {
//            if (mAspectX > mAspectY)
//            {
//                cropHeight = cropWidth * mAspectY / mAspectX;
//            }
//            else
//            {
//                cropWidth = cropHeight * mAspectX / mAspectY;
//            }
//        }
//
//        int x = (width - cropWidth) / 2;
//        int y = (height - cropHeight) / 2;

        RectF cropRect = new RectF(rect);

        Log.d("/!", "getImageViewMatrix() = " + getImageViewMatrix());

        HighlightView hv = new HighlightView(this, mHighlight);
        hv.setup(getImageViewMatrix(), imageRect, cropRect, mCircleCrop, mAspectX != 0 && mAspectY != 0);

        return hv;
    }

    private static class FaceDetectorTask implements Callable<List<HighlightView>>
    {
        CropImageView mImageView;

        public FaceDetectorTask(CropImageView imageView)
        {
            mImageView = imageView;
        }

        // Scale the image down for faster face detection.
        private Pair<Bitmap, Float> prepareFaceBitmap()
        {
            Bitmap bitmap = mImageView.getBaseBitmap();
            if (bitmap == null || bitmap.isRecycled())
            {
                return null;
            }

            // 256 pixels wide is enough.
            float scale = 1;
            if (bitmap.getWidth() > 256)
            {
                scale = 256.0F / bitmap.getWidth();
            }

            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale);

            Bitmap faceBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            Bitmap b565       = faceBitmap.copy(Bitmap.Config.RGB_565, true);

            faceBitmap.recycle();
            return new Pair<>(b565, scale);
        }

        public List<HighlightView> call() throws Exception
        {
            Pair<Bitmap, Float> pair = prepareFaceBitmap();
            if (pair == null) throw new IllegalStateException();

            Bitmap bitmap = pair.first;
            float  scale  = 1.0F / pair.second;

            FaceDetector.Face[] faces = new FaceDetector.Face[5];
            int                 numFaces;

            FaceDetector detector = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), faces.length);
            numFaces = detector.findFaces(bitmap, faces);

            bitmap.recycle();

            List<HighlightView> hvs = new ArrayList<>();
            if (numFaces != 0)
            {
                for (int i = 0; i < numFaces; i++)
                {
                    hvs.add(mImageView.createHighlightForFace(faces[i], scale));
                }
            }

            return hvs;
        }
    }
}