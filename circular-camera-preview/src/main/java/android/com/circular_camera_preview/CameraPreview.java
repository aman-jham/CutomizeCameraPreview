package android.com.circular_camera_preview;

import android.app.Activity;
import android.com.circular_camera_preview.config.CameraFacing;
import android.com.circular_camera_preview.config.CameraImageFormat;
import android.com.circular_camera_preview.config.CameraResolution;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.SortedSet;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private Context mContext;
    private Paint paint;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Path clipPath;
    private int coordinateX = 0, coordinateY = 0;
    private int radius = 0;
    private CameraConfig mCameraConfig;
    private SizeMap mPictureSizes = new SizeMap();
    private AspectRatio mAspectRatio;
    private SizeMap mPreviewSizes = new SizeMap();
    private int mOrientation = 0;
    private Bitmap bitmap;
    private CameraCallbacks mCameraCallbacks;
    private Runnable mCameraRunnable;
    private Handler mHandler;
    private ByteArrayOutputStream outstr;
    private Rect rect;
    private YuvImage yuvimage;
    private Size size = new Size(0, 0);

    public CameraPreview(Context context) {
        super(context);
        this.mContext = context;
        initialize();
    }

    private void initialize() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        clipPath = new Path();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        setDrawingCacheEnabled(true);
        initRunnable();
        if (!isInEditMode())
            startPreview();
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        initialize();
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initialize();
    }

    public void initConfig(Context context) {
        mCameraConfig = new CameraConfig()
                .getBuilder(context)
                .setCameraFacing(CameraFacing.FRONT_FACING_CAMERA)
                .setCameraResolution(CameraResolution.HIGH_RESOLUTION)
                .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                .setImageRotation(mOrientation)
                .build();
    }

    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) {
            return;
        }
        startPreview();
    }

    private void initRunnable() {
        mHandler = new Handler();
        mCameraRunnable = new Runnable() {
            @Override
            public void run() {
                stopPreview();
                try {
                    mCamera = getCameraInstance();
                    if (mCamera == null)
                        return;
                    Camera.Parameters parameters = mCamera.getParameters();
                    mPreviewSizes.clear();
                    for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                        mPreviewSizes.add(new Size(size.width, size.height));
                    }

                    // Supported picture sizes;
                    mPictureSizes.clear();
                    for (Camera.Size size : parameters.getSupportedPictureSizes()) {
                        mPictureSizes.add(new Size(size.width, size.height));
                    }
                    // AspectRatio
                    if (mAspectRatio == null) {
                        mAspectRatio = Constant.DEFAULT_ASPECT_RATIO;
                    }

                    adjustCameraParameters(parameters);

                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        };
    }

    public void startPreview() {
        stopPreview();
        mHandler.post(mCameraRunnable);
    }

    public void stopPreview() {
        try {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
        } catch (Exception ignored) {
        }

        try {
            mHandler.removeCallbacks(mCameraRunnable);
        } catch (Exception ignored) {
        }
    }

    void adjustCameraParameters(Camera.Parameters mCameraParameters) {
        SortedSet<Size> sizes = mPreviewSizes.sizes(mAspectRatio);
        if (sizes == null) {
            mAspectRatio = chooseAspectRatio();
            sizes = mPreviewSizes.sizes(mAspectRatio);
        }

        size = chooseOptimalSize(sizes);

        Size pictureSize = chooseOptimalSize(mPictureSizes.sizes(mAspectRatio));

        mCameraParameters.setPreviewSize(size.getWidth(), size.getHeight());
        mCameraParameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
//        mCameraParameters.setRotation(mCameraConfig.getmImageRotation());
        try {
            mHolder = getHolder();
            mHolder.setSizeFromLayout();
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setParameters(mCameraParameters);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getOrientation(Camera.CameraInfo info) {
        int rotation = ((WindowManager) mContext.getSystemService(Activity.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private AspectRatio chooseAspectRatio() {
        AspectRatio r = null;
        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            r = ratio;
            if (ratio.equals(Constant.DEFAULT_ASPECT_RATIO)) {
                return ratio;
            }
        }
        return r;
    }

    private Size chooseOptimalSize(SortedSet<Size> sizes) {
        if (getWidth() == 0 || getHeight() == 0) { // Not yet laid out
            return sizes.first(); // Return the smallest size
        }
        int desiredWidth;
        int desiredHeight;
        final int surfaceWidth = getWidth();
        final int surfaceHeight = getHeight();
        if (isLandscape(mCameraConfig.getmImageRotation())) {
            desiredWidth = surfaceHeight;
            desiredHeight = surfaceWidth;
        } else {
            desiredWidth = surfaceWidth;
            desiredHeight = surfaceHeight;
        }
        Size result = null;
        for (Size size : sizes) {
            if (desiredWidth <= size.getWidth() && desiredHeight <= size.getHeight()) {
                return size;
            }
            result = size;
        }
        return result;
    }

    private boolean isLandscape(int orientationDegrees) {
        return (orientationDegrees == 90 ||
                orientationDegrees == 270);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
       /* clipPath.addCircle(coordinateX, coordinateY, radius, Path.Direction.CCW);
        canvas.clipPath(clipPath);
        canvas.drawPath(clipPath, paint);
        setZOrderMediaOverlay(true);*/
        super.dispatchDraw(canvas);
    }

    public void setCenterPoint(int coordinateX, int coordinateY, int radius) {
        this.coordinateX = coordinateX;
        this.coordinateY = coordinateY;
        this.radius = radius;
    }

    public void takePicture() {

        if (mCamera != null) {
            //mCamera.setPreviewCallback(null);
            try {
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(final byte[] bytes, final Camera camera) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                stopPreview();
                                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                validateRotation();
                                if (HiddenCameraUtils.saveImageFromFile(bitmap,
                                        mCameraConfig.getImageFile(),
                                        mCameraConfig.getImageFormat())) {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mCameraCallbacks == null)
                                                return;
                                            mCameraCallbacks.onImageTaken(mCameraConfig.getImageFile(), bitmap);
                                        }
                                    });
                                } else {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mCameraCallbacks == null)
                                                return;
                                            mCameraCallbacks.onCameraError(CameraError.ERROR_IMAGE_WRITE_FAILED);
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                });
            } catch (Exception ignored) {
            }
        } else {
            if (mCameraCallbacks == null)
                return;
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        outstr = new ByteArrayOutputStream();
        rect = new Rect(0, 0, size.getWidth(), size.getHeight());
        yuvimage = new YuvImage(data, ImageFormat.NV21, size.getWidth(), size.getHeight(), null);
        yuvimage.compressToJpeg(rect, 50, outstr);
        bitmap = BitmapFactory.decodeByteArray(outstr.toByteArray(), 0, outstr.size());
        validateRotation();
        if (mCameraCallbacks == null)
            return;
        if (bitmap != null)
            mCameraCallbacks.onImagePreview(data, bitmap, mCameraConfig.getmImageRotation());
        else
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//            matrix.preScale(-1.0f, 1.0f);
            matrix.postRotate(90.f);
        }
        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
//        bm.recycle();
        return resizedBitmap;
    }

    public byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, buffer);
        return buffer.toByteArray();
    }

    private Bitmap validateRotation() {
        if (bitmap == null)
            return bitmap;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Matrix mtx = new Matrix();
            mtx.preScale(-1.0f, 1.0f);
            mtx.postRotate(90.f);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mtx, true);
        } else {
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        }
        return bitmap;
    }


    public Camera getCameraInstance() {
        Camera c = null;
        try {
            Camera.CameraInfo info = new Camera.CameraInfo();
            int count = Camera.getNumberOfCameras();
            for (int i = 0; i < count; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    try {
                        c = Camera.open(i);
                        mOrientation = getOrientation(info);
                        initConfig(mContext);
                        c.setDisplayOrientation(mOrientation);
                    } catch (RuntimeException ignored) {
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    public void addCallback(CameraCallbacks mCameraCallbacks) {
        this.mCameraCallbacks = mCameraCallbacks;
    }

    public interface CameraCallbacks {

        void onImagePreview(byte[] bytes, Bitmap bitmap, int rotationAngle);

        void onImageTaken(File imageFile, Bitmap bitmap);

        void onCameraError(@CameraError.CameraErrorCodes int errorCode);
    }
}