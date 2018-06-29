package android.com.circular_camera_preview;

import android.graphics.Bitmap;

import java.io.File;

public interface CircaularCameraCallBacks {

    void onImagePreview(byte[] data, Bitmap bitmap);

    void onImageTaken(File f, Bitmap bitmap);

    void onCapturing();

}
