package com.wangshuo.videostrategy.videostrategy.strategy.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;

import com.wangshuo.videostrategy.videostrategy.strategy.encoder.MediaMuxerWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraProxy {

    private static final String TAG = "CameraProxy";
    private boolean isDebug = true;

    private Context mContext;
    private int mCameraId;
    private Camera mCamera;

    private boolean isCameraOpen = false;
    private boolean mCameraOpenFailed = false;

    private SurfaceTexture mSurfaceTexture;

    private int mCameraDirection;

    private CameraInfo mCameraInfo = new CameraInfo();

    public CameraProxy(Context context) {
        mContext = context;
        MediaMuxerWrapper.setContext(context);
    }

    public Camera getCamera() {
        return mCamera;
    }

    public boolean openCamera(int cameraId) {
        try {
            releaseCamera();
            mCamera = Camera.open(cameraId);
            mCamera.getParameters();
            mCameraId = cameraId;
            mCamera.getCameraInfo(cameraId, mCameraInfo);

            setDefaultParameters();
            setCameraDisplayOrientation();

            isCameraOpen = true;
            mCameraOpenFailed = false;
        } catch (Exception e) {
            mCameraOpenFailed = true;
            mCamera = null;
            Log.i(TAG, "openCamera fail msg=" + e.getMessage());
            return false;
        }
        return true;
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void setCameraDisplayOrientation() {
        if (mContext instanceof Activity) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(mCameraId, info);

            int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
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
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;
            } else {
                result = (info.orientation - degrees + 360) % 360;
            }
            mCamera.setDisplayOrientation(result);
        }
    }


    public void startPreview(SurfaceTexture surfaceTexture, PreviewCallback previewcallback) {
        try {
            if (mCamera == null) {
                return;
            }
            if (surfaceTexture != null && mCamera != null) {
            }
            mCamera.setPreviewTexture(surfaceTexture);

            if (previewcallback != null && mCamera != null) {
                mCamera.setPreviewCallback(previewcallback);
            }
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startPreview() {
        if (mCamera != null)
            mCamera.startPreview();
    }

    public void stopPreview() {
        if (mCamera != null)
            mCamera.stopPreview();
    }

    public Size getPreviewSize() {
        if (mCamera != null) {
            return mCamera.getParameters().getPreviewSize();
        }
        return null;
    }

    public void setOneShotPreviewCallback(PreviewCallback callback) {
        mCamera.setOneShotPreviewCallback(callback);
    }

    public void addPreviewCallbackBuffer(byte[] callbackBuffer) {
        mCamera.addCallbackBuffer(callbackBuffer);
    }


    public int getOrientation() {
        if (mCameraInfo == null) {
            return 0;
        }
        return mCameraInfo.orientation;
    }

    public boolean isFlipHorizontal() {
        if (mCameraInfo == null) {
            return false;
        }
        return mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT ? true : false;
    }

    public int getCameraId() {
        return mCameraId;
    }

    public boolean isFrontCamera() {
        return mCameraId == CameraInfo.CAMERA_FACING_FRONT;
    }

    public void setRotation(int rotation) {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            params.setRotation(rotation);
            mCamera.setParameters(params);
        }
    }

    public void takePicture(Camera.ShutterCallback shutterCallback, Camera.PictureCallback rawCallback,
                            Camera.PictureCallback jpegCallback) {
        if (mCamera != null) {
            mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
        }
    }

    public int getDisplayOrientation(int dir) {
        /**
         * 请注意前置摄像头与后置摄像头旋转定义不同
         * 请注意不同手机摄像头旋转定义不同
         */
        int newdir = dir;
        if (isFrontCamera() &&
                ((mCameraInfo.orientation == 270 && (dir & 1) == 1) ||
                        (mCameraInfo.orientation == 90 && (dir & 1) == 0))) {
            newdir = (dir ^ 2);
        }
        return newdir;
    }

    public boolean needMirror() {
        if (isFrontCamera()) {
            return true;
        } else {
            return false;
        }
    }

    private void setDefaultParameters() {
        Parameters parameters = mCamera.getParameters();
        Log.e(TAG, "parameters: " + parameters.flatten());
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        parameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null) {
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            }
        }

        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(Parameters.FLASH_MODE_OFF)) {
            parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
        }

        Point previewSize = getSuitablePreviewSize();
        //	parameters.setPreviewSize(previewSize.x, previewSize.y);
        parameters.setPreviewSize(1280, 720);

       // parameters.set("orientation", "landscape");

        selectCameraFpsRange(parameters);
        if(previewMaxFps/1000 >= 30){
            parameters.set("preview-frame-rate", "30");
        }else {
            parameters.set("preview-frame-rate", String.valueOf(previewMaxFps/1000));
        }
        parameters.setPreviewFpsRange(previewMinFps, previewMaxFps);

        mCamera.setParameters(parameters);
    }

    public Parameters getParameters() {
        return mCamera.getParameters();
    }

    public void setPreviewSize(int width, int height) {
        if (mCamera == null)
            return;
        Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(width, height);
        if (mCamera == null)
            return;
        mCamera.setParameters(parameters);
    }

    private Point getSuitablePreviewSize() {
        Point defaultsize = new Point(1920, 1080);
        if (mCamera != null) {
            List<Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
            for (Size s : sizes) {
                if ((s.width == defaultsize.x) && (s.height == defaultsize.y)) {
                    return defaultsize;
                }
            }
            return new Point(640, 480);
        }
        return null;
    }

    public ArrayList<String> getSupportedPreviewSize(String[] previewSizes) {
        ArrayList<String> result = new ArrayList<String>();
        if (mCamera != null) {
            List<Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
            for (String candidate : previewSizes) {
                int index = candidate.indexOf('x');
                if (index == -1) continue;
                int width = Integer.parseInt(candidate.substring(0, index));
                int height = Integer.parseInt(candidate.substring(index + 1));
                for (Size s : sizes) {
                    if ((s.width == width) && (s.height == height)) {
                        result.add(candidate);
                    }
                }
            }
        }
        return result;
    }

    public int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    public boolean cameraOpenFailed() {
        return mCameraOpenFailed;
    }

    public boolean isCameraOpen() {
        return isCameraOpen;
    }

    public static int targetFps = 30000;
    private static int previewMinFps = 20000;
    private static int previewMaxFps = 30000;

    public static void selectCameraFpsRange(Camera.Parameters parameters) {
        List<int[]> fpsRanges = parameters.getSupportedPreviewFpsRange();
        Collections.sort(fpsRanges, new Comparator<int[]>() {
            @Override
            public int compare(int[] lhs, int[] rhs) {
                int r = Math.abs(lhs[0] - targetFps) + Math.abs(lhs[1] - targetFps);
                int l = Math.abs(rhs[0] - targetFps) + Math.abs(rhs[1] - targetFps);
                if (r > l) {
                    return 1;
                } else if (r < l) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        previewMinFps = fpsRanges.get(0)[0];
        previewMaxFps = fpsRanges.get(0)[1];
    }

}
