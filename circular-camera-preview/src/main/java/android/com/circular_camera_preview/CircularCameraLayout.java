package android.com.circular_camera_preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;

import java.io.File;

public class CircularCameraLayout extends CardView implements View.OnClickListener, CameraPreview.CameraCallbacks {
    private CameraPreview mPreview;
    private ImageView mCivImage = null;
    private ProgressBar mProgress;
    private TextView mTextView;
    private boolean isRemoved = false;
    private File mImage = null;
    private Bitmap mClickedBitmap = null;
    private RelativeLayout rlContainer;
    private CountDownTimer countDownTimer;
    private Handler handler;

    public void setCircularCameraCallBacks(CircaularCameraCallBacks circaularCameraCallBacks) {
        this.circaularCameraCallBacks = circaularCameraCallBacks;
    }


    public void setStartPreview() {
        mProgress.setVisibility(View.VISIBLE);
        if (!isRemoved) {
            mPreview.setVisibility(VISIBLE);
            mPreview.startPreview();
            isRemoved = true;
            mProgress.setVisibility(View.GONE);
            if (circaularCameraCallBacks != null)
                circaularCameraCallBacks.onCapturing();

            mPreview.performClick();
        }
    }


    public void stopPreview() {
        if (isRemoved) {
            mPreview.stopPreview();
            isRemoved = false;
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
            mTextView.setVisibility(GONE);
            isRemoved = false;

            if (circaularCameraCallBacks != null)
                circaularCameraCallBacks.onCapturing();
        }
    }

    private CircaularCameraCallBacks circaularCameraCallBacks;

    public CircularCameraLayout(Context context) {
        super(context);
        setup();
    }

    public CircularCameraLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public CircularCameraLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void initCircularImageView() {
        mCivImage = new CIVImage(getContext());
        RelativeLayout.LayoutParams mLayoutParams = new RelativeLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mCivImage.setLayoutParams(mLayoutParams);
        //mCivImage.setImageResource(R.drawable.flat_icon_new_user);
        rlContainer.addView(mCivImage);
    }

    private void initProgress() {
        mProgress = new ProgressBar(getContext());
        RelativeLayout.LayoutParams mLayoutParams = new RelativeLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        mLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mProgress.setLayoutParams(mLayoutParams);
        mProgress.setVisibility(View.GONE);
        rlContainer.addView(mProgress);
    }

    private void initTextSwitcher() {
        mTextView = new TextView(getContext());
        RelativeLayout.LayoutParams mLayoutParams = new RelativeLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        mLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mTextView.setLayoutParams(mLayoutParams);
        mTextView.setVisibility(View.GONE);
        mTextView.setTextColor(Color.YELLOW);
        mTextView.setGravity(Gravity.CENTER);
        mTextView.setTextSize(getResources().getDimensionPixelSize(R.dimen._15sdp));
        rlContainer.addView(mTextView);
    }


    private void setup() {
        init();
        initContainer();
        initCircularImageView();
        initProgress();
        initLayout();
        initTextSwitcher();
        invalidate();
    }

    private void init() {
        handler = new Handler();
    }

    private void initContainer() {
        rlContainer = new RelativeLayout(getContext());
        RelativeLayout.LayoutParams mLayoutParams = new RelativeLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        rlContainer.setLayoutParams(mLayoutParams);
        addView(rlContainer);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void initLayout() {
        setOnClickListener(this);
        if (isInEditMode())
            return;
        mPreview = new CameraPreview(getContext());
        mPreview.addCallback(this);
        mPreview.setOnClickListener(this);
        mPreview.setVisibility(View.GONE);
        rlContainer.addView(mPreview, 0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (null != mPreview)
            mPreview.setCenterPoint((getHeight() - (getContentPaddingLeft() + getContentPaddingRight())) / 2, (getWidth() - (getPaddingLeft() + getPaddingRight())) / 2, (getWidth() - (getContentPaddingLeft() + getContentPaddingRight())) / 2);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            countDownTimer = new CountDownTimer(5000, 1000) {
                @Override
                public void onTick(final long millisUntilFinished) {

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            mTextView.setTextSize(getResources().getDimensionPixelSize(R.dimen._13sdp));
                            System.out.println("millisUntilFinished " + millisUntilFinished);
                            int seconds = (int) (millisUntilFinished / 1000) % 60;
                            mTextView.setText("" + seconds);
                            mTextView.setVisibility(VISIBLE);
                        }
                    });
                }

                @Override
                public void onFinish() {
                    if (isRemoved) {
                        mTextView.setVisibility(GONE);
                        mPreview.initConfig(getContext());
                        mProgress.setVisibility(View.VISIBLE);
                        mPreview.takePicture();
                        if (circaularCameraCallBacks != null)
                            circaularCameraCallBacks.onCapturing();

                        isRemoved = false;
                        return;
                    }
                }
            }.start();
        }
    };

    private void callTimer() {
        mTextView.setTextSize(getResources().getDimensionPixelSize(R.dimen._7sdp));
        mTextView.setText("Smile");
        mTextView.setVisibility(VISIBLE);
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, 1500);
    }


    @Override
    public void onClick(View v) {

        if (!isRemoved) {
            setStartPreview();
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
            isRemoved = false;
            mTextView.setVisibility(GONE);
            return;
        }

        callTimer();
    }

    public void showText() {
        mTextView.setTextSize(getResources().getDimensionPixelSize(R.dimen._7sdp));
        mTextView.setTextColor(Color.YELLOW);
        mTextView.setVisibility(View.VISIBLE);
        mTextView.setText("You look great");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mTextView.setTextSize(getResources().getDimensionPixelSize(R.dimen._15sdp));
                mTextView.setText("");
                mTextView.setVisibility(View.GONE);
            }
        }, 2000);
    }

    public File getFile() {
        return mImage;
    }

    public Bitmap getBitmap() {
        return mClickedBitmap;
    }

    @Override
    public void onImagePreview(final byte[] data, Bitmap bitmap, final int rotationAngle) {
        try {
            mClickedBitmap = bitmap;
            Glide.with(getContext())
                    .load(data)
                    .dontAnimate()
                    .signature(new StringSignature(String.valueOf(System.currentTimeMillis())))
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .bitmapTransform(new RotateTransformation(getContext(), rotationAngle))
                    .into(mCivImage);
//            mCivImage.setImageBitmap(bitmap);
//            mCivImage.setRotation(rotationAngle);
//            bitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (circaularCameraCallBacks != null)
            circaularCameraCallBacks.onImagePreview(data, bitmap);
        mProgress.setVisibility(View.GONE);
    }

    @Override
    public void onImageTaken(File imageFile, Bitmap bitmap) {
        try {
            if (imageFile != null && imageFile.exists())
                Glide.with(getContext()).load("file:///" + imageFile.getAbsolutePath()).crossFade().into(mCivImage);
        } catch (Exception ignored) {
        }
        if (circaularCameraCallBacks != null)
            circaularCameraCallBacks.onImageTaken(imageFile, bitmap);
        mPreview.setVisibility(View.GONE);
        mProgress.setVisibility(View.GONE);
    }

    @Override
    public void onCameraError(int errorCode) {
        mProgress.setVisibility(View.GONE);
    }
}
