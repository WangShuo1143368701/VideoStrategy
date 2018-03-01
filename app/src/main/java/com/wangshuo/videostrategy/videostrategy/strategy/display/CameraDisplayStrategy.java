package com.wangshuo.videostrategy.videostrategy.strategy.display;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Build;

import com.wangshuo.videostrategy.videostrategy.strategy.camera.CameraProxy;
import com.wangshuo.videostrategy.videostrategy.strategy.encoder.MediaAudioEncoder;
import com.wangshuo.videostrategy.videostrategy.strategy.encoder.MediaEncoder;
import com.wangshuo.videostrategy.videostrategy.strategy.encoder.MediaMuxerWrapper;
import com.wangshuo.videostrategy.videostrategy.strategy.encoder.MediaVideoEncoder;
import com.wangshuo.videostrategy.videostrategy.strategy.filter.basefilter.BaseVideoFilter;
import com.wangshuo.videostrategy.videostrategy.strategy.glutils.GlUtil;
import com.wangshuo.videostrategy.videostrategy.strategy.glutils.OpenGLUtils;
import com.wangshuo.videostrategy.videostrategy.strategy.glutils.TextureRotationUtil;
import com.wangshuo.videostrategy.videostrategy.strategy.utils.Accelerometer;
import com.wangshuo.videostrategy.videostrategy.strategy.utils.LogUtils;
import com.wangshuo.videostrategy.videostrategy.strategy.utils.VideoStrategyContents;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * CameraDisplayStrategy is used for camera preview
 */
public class CameraDisplayStrategy implements Renderer {

    private String TAG = "CameraDisplayStrategy";
    private int previewWidth = 1280;
    private int previewHeight = 720;
    private int recordWidth = 960;
    private int recordHeight = 544;
    private int recordFps = 20;
    private File savaImageFile = null;
    /**
     * SurfaceTexure texture id
     */
    protected int mTextureId = OpenGLUtils.NO_TEXTURE;
    private int mImageWidth;
    private int mImageHeight;
    private GLSurfaceView mGlSurfaceView;
    private ChangePreviewSizeListener mListener;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private Context mContext;
    public CameraProxy mCameraProxy;
    private SurfaceTexture mSurfaceTexture;

    private int mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private STGLRender mGLRender;

    private ByteBuffer mRGBABuffer;
    private int[] mTextureOutId;
    private boolean mCameraChanging = false;
    private boolean mSetPreViewSizeSucceed = false;
    private boolean mIsChangingPreviewSize = false;

    private long mStartTime;
    private boolean mNeedSave = false;
    private FloatBuffer mTextureBuffer;

    private boolean mIsPaused = false;
    private Object mImageDataLock = new Object();

    private byte[] mRotateData;
    private byte[] mNv21ImageData;

    private byte[] mImageData;

    //for test fps
    private float mFps;
    private int mCount = 0;
    private long mCurrentTime = 0;
    private boolean mIsFirstCount = true;
    private int mFrameCost = 0;

    private MediaVideoEncoder mVideoEncoder;
    private final float[] mStMatrix = new float[16];
    private int[] mVideoEncoderTexture;
    private boolean mNeedResetEglContext = false;
    private int encodeFrameCount;         //当前录制的帧数
    private OnFrameDrawListener onFrameDrawListener;

    private BaseVideoFilter mVideoFilter;
    private boolean isOpenFilter;

    public interface ChangePreviewSizeListener {
        void onChangePreviewSize(int previewW, int previewH);
    }

    public CameraDisplayStrategy(Context context, ChangePreviewSizeListener listener, GLSurfaceView glSurfaceView) {
        mCameraProxy = new CameraProxy(context);
        mGlSurfaceView = glSurfaceView;
        mListener = listener;
        mContext = context;
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(this);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        mTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);
        mGLRender = new STGLRender();
    }

    /**
     * 工作在opengl线程, 当前Renderer关联的view创建的时候调用
     *
     * @param gl
     * @param config
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtils.i(TAG, "onSurfaceCreated");
        if (mIsPaused == true) {
            return;
        }
        GLES20.glEnable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);

        while (!mCameraProxy.isCameraOpen()) {
            if (mCameraProxy.cameraOpenFailed()) {
                return;
            }
            try {
                Thread.sleep(10, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (mCameraProxy.getCamera() != null) {
            setUpCamera();
        }
    }

    /**
     * 工作在opengl线程, 当前Renderer关联的view尺寸改变的时候调用
     *
     * @param gl
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        LogUtils.i(TAG, "onSurfaceChanged");
        if (mIsPaused == true) {
            return;
        }
        adjustViewPort(width, height);

        mGLRender.init(mImageWidth, mImageHeight);
        mStartTime = System.currentTimeMillis();
    }

    /**
     * 根据显示区域大小调整一些参数信息
     *
     * @param width
     * @param height
     */
    private void adjustViewPort(int width, int height) {
        mSurfaceHeight = height;
        mSurfaceWidth = width;
        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        mGLRender.calculateVertexBuffer(mSurfaceWidth, mSurfaceHeight, mImageHeight, mImageWidth);
    }

    long lastMs = 0;
    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, Camera camera) {

            long nowMs = System.currentTimeMillis();
            LogUtils.e("ws", "onPreviewFrame: " + (nowMs - lastMs) + " ms");
            lastMs = nowMs;

            if (mCameraChanging || mCameraProxy.getCamera() == null) {
                return;
            }

            if (mRotateData == null || mRotateData.length != mImageHeight * mImageWidth * 3 / 2) {
                mRotateData = new byte[mImageHeight * mImageWidth * 3 / 2];
            }

            //int orientation = getRotateOrientation();


            if (mImageData == null || mImageData.length != mImageHeight * mImageWidth * 3 / 2) {
                mImageData = new byte[mImageWidth * mImageHeight * 3 / 2];
            }
            synchronized (mImageDataLock) {
                System.arraycopy(data, 0, mImageData, 0, data.length);
            }

            mGlSurfaceView.requestRender();
        }
    };

    /**
     * 工作在opengl线程, 具体渲染的工作函数
     *
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        // during switch camera
        if (mCameraChanging) {
            return;
        }

        if (mCameraProxy.getCamera() == null) {
            return;
        }

        LogUtils.w(TAG, "onDrawFrame");
        if (mRGBABuffer == null) {
            mRGBABuffer = ByteBuffer.allocate(mImageHeight * mImageWidth * 4);
        }

        if (mTextureOutId == null) {
            mTextureOutId = new int[1];
            GlUtil.initEffectTexture(mImageHeight, mImageWidth, mTextureOutId, GLES20.GL_TEXTURE_2D);
        }

        if (mVideoEncoderTexture == null) {
            mVideoEncoderTexture = new int[1];
        }

        if (mSurfaceTexture != null && !mIsPaused) {
            mSurfaceTexture.updateTexImage();
        } else {
            return;
        }

        mStartTime = System.currentTimeMillis();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mRGBABuffer.rewind();

        long preProcessCostTime = System.currentTimeMillis();
        int textureId = mGLRender.preProcess(mTextureId, null);

        if (isOpenFilter && mVideoFilter != null) {
                textureId = mGLRender.drawFilterBuffer(textureId, mVideoFilter);
        }

        //int orientation = getCurrentOrientation();

        if (mNeedSave && savaImageFile != null) {
            savePicture(textureId,savaImageFile);
            mNeedSave = false;
        }

        if (mVideoEncoder != null) {
            GLES20.glFinish();
        }

        mVideoEncoderTexture[0] = textureId;
        mSurfaceTexture.getTransformMatrix(mStMatrix);
        processStMatrix(mStMatrix, mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT);

        synchronized (this) {
            if (mVideoEncoder != null) {
                if (mNeedResetEglContext) {
                    mVideoEncoder.setEglContext(EGL14.eglGetCurrentContext(), mVideoEncoderTexture[0]);
                    mNeedResetEglContext = false;
                }
                mVideoEncoder.setPreviewWH(mImageHeight, mImageWidth);
                mVideoEncoder.frameAvailableSoon(mStMatrix, mVideoEncoder.getMvpMatrix());

                if (onFrameDrawListener != null) {
                    onFrameDrawListener.onFrameDraw(encodeFrameCount);
                }

                encodeFrameCount++;
            } else {
                encodeFrameCount = 0;           //重置
            }
        }

        mFrameCost = (int) (System.currentTimeMillis() - mStartTime / 20);

        long timer = System.currentTimeMillis();
        mCount++;
        if (mIsFirstCount) {
            mCurrentTime = timer;
            mIsFirstCount = false;
        } else {
            int cost = (int) (timer - mCurrentTime);
            if (cost >= 1000) {
                mCurrentTime = timer;
                mFps = (((float) mCount * 1000) / cost) + 0.5f;
                mCount = 0;
            }
        }

        LogUtils.w(TAG, "render fps: %f", mFps);

        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);

        mGLRender.onDrawFrame(textureId);
        LogUtils.w(TAG, "onDrawFrame time total %d", System.currentTimeMillis() - preProcessCostTime);
    }

    private void savePicture(int textureId,File file) {
        ByteBuffer mTmpBuffer = ByteBuffer.allocate(mImageHeight * mImageWidth * 4);
        mGLRender.saveTextureToFrameBuffer(textureId, mTmpBuffer);

        mTmpBuffer.position(0);
        onPictureTaken(mTmpBuffer,file,mImageWidth,mImageHeight);
    }

    private void onPictureTaken(ByteBuffer data, File file, int mImageWidth, int mImageHeight) {
        if (mImageWidth <= 0 || mImageHeight <= 0)
            return;
        Bitmap srcBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        data.position(0);
        srcBitmap.copyPixelsFromBuffer(data);
        saveToSDCard(file, srcBitmap);
        srcBitmap.recycle();
    }


    private void saveToSDCard(File file, Bitmap bmp) {

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bos != null)
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

            String path = file.getAbsolutePath();
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            mContext.sendBroadcast(mediaScanIntent);

            if (Build.VERSION.SDK_INT >= 19) {

                MediaScannerConnection.scanFile(mContext, new String[]{path}, null, null);
            }

    }

    private int getCurrentOrientation() {
        int dir = Accelerometer.getDirection();
        int orientation = dir - 1;
        if (orientation < 0) {
            orientation = dir ^ 3;
        }

        return orientation;
    }

    /**
     * camera设备startPreview
     */
    private void setUpCamera() {
        // 初始化Camera设备预览需要的显示区域(mSurfaceTexture)
        if (mTextureId == OpenGLUtils.NO_TEXTURE) {
            mTextureId = OpenGLUtils.getExternalOESTextureID();
            mSurfaceTexture = new SurfaceTexture(mTextureId);
        }

        if (mIsPaused)
            return;

        while (!mSetPreViewSizeSucceed) {
            try {
                mCameraProxy.setPreviewSize(previewWidth, previewHeight);
                mSetPreViewSizeSucceed = true;
            } catch (Exception e) {
                mSetPreViewSizeSucceed = false;
            }

            try {
                Thread.sleep(10);
            } catch (Exception e) {

            }
        }
        mImageHeight = previewHeight;
        mImageWidth = previewWidth;

        boolean flipHorizontal = mCameraProxy.isFlipHorizontal();

        if (mCameraProxy.getCameraId() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mGLRender.adjustTextureBuffer(270, flipHorizontal);
        } else {
            mGLRender.adjustTextureBuffer(90, flipHorizontal);
        }


        if (mIsPaused)
            return;
        mCameraProxy.startPreview(mSurfaceTexture, mPreviewCallback);
    }

    public void onResume() {
        LogUtils.i(TAG, "onResume");

        if (mCameraProxy.getCamera() == null) {
            if (mCameraProxy.getNumberOfCameras() == 1) {
                mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
            mCameraProxy.openCamera(mCameraID);
        }
        mIsPaused = false;
        mSetPreViewSizeSucceed = false;

        mGLRender = new STGLRender();

        mGlSurfaceView.onResume();
        mGlSurfaceView.forceLayout();
        //mGlSurfaceView.requestRender();
    }

    public void onPause() {
        LogUtils.i(TAG, "onPause");
        mSetPreViewSizeSucceed = false;
        mIsPaused = true;
        mImageData = null;
        mCameraProxy.releaseCamera();
        LogUtils.d(TAG, "Release camera");

        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRGBABuffer = null;
                mNv21ImageData = null;
                deleteTextures();
                if (mSurfaceTexture != null) {
                    mSurfaceTexture.release();
                }
                mGLRender.destroyFrameBuffers();
            }
        });

        mGlSurfaceView.onPause();
    }

    public void onDestroy() {
        //必须释放非opengGL句柄资源,负责内存泄漏

    }

    /**
     * 释放纹理资源
     */
    protected void deleteTextures() {
        LogUtils.i(TAG, "delete textures");
        deleteCameraPreviewTexture();
        deleteInternalTextures();
    }

    // must in opengl thread
    private void deleteCameraPreviewTexture() {
        if (mTextureId != OpenGLUtils.NO_TEXTURE) {
            GLES20.glDeleteTextures(1, new int[]{
                    mTextureId
            }, 0);
        }
        mTextureId = OpenGLUtils.NO_TEXTURE;
    }

    private void deleteInternalTextures() {
        if (mTextureOutId != null) {
            GLES20.glDeleteTextures(1, mTextureOutId, 0);
            mTextureOutId = null;
        }

        if (mVideoEncoderTexture != null) {
            GLES20.glDeleteTextures(1, mVideoEncoderTexture, 0);
            mVideoEncoderTexture = null;
        }
    }

    //----------------- 相机api --------------------
    /**
     * 切换前后置
     * @return
     */
    public int switchCamera() {
        if (Camera.getNumberOfCameras() == 1
                || mCameraChanging) {
            return Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        if (mCameraProxy.cameraOpenFailed()) {
            return -1;
        }

        final int cameraID = 1 - mCameraID;
        mCameraChanging = true;
        mCameraProxy.openCamera(cameraID);

        mSetPreViewSizeSucceed = false;

        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                deleteTextures();
                if (mCameraProxy.getCamera() != null) {
                    setUpCamera();
                }
                mCameraChanging = false;
                mCameraID = cameraID;
            }
        });

        return cameraID;
    }

    /**
     * 获取摄像头id
     * @return
     */
    public int getCameraID() {
        return mCameraID;
    }

    /**
     * 获取相机预览宽
     * @return
     */
    public int getPreviewWidth() {
        return mImageWidth;
    }

    /**
     * 获取相机预览高
     * @return
     */
    public int getPreviewHeight() {
        return mImageHeight;
    }

    /**
     * 是否修改预览分辨率中
     * @return
     */
    public boolean isChangingPreviewSize() {
        return mIsChangingPreviewSize;
    }

    /**
     * 更改预览分辨率
     * @param previewWidth
     * @param previewHeight
     */
    public void changePreviewSize(int previewWidth ,int previewHeight) {
        if (mCameraProxy.getCamera() == null || mCameraChanging
                || mIsPaused) {
            return;
        }

        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;

        mSetPreViewSizeSucceed = false;
        mIsChangingPreviewSize = true;

        mCameraChanging = true;
        mCameraProxy.stopPreview();
        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mRGBABuffer != null) {
                    mRGBABuffer.clear();
                }
                mRGBABuffer = null;

                deleteTextures();
                if (mCameraProxy.getCamera() != null) {
                    setUpCamera();
                }

                mGLRender.init(mImageHeight, mImageWidth);
                mGLRender.calculateVertexBuffer(mSurfaceWidth, mSurfaceHeight, mImageHeight, mImageWidth);
                if (mListener != null) {
                    mListener.onChangePreviewSize(mImageHeight, mImageWidth);
                }

                mCameraChanging = false;
                mIsChangingPreviewSize = false;
                //mGlSurfaceView.requestRender();
                LogUtils.d(TAG, "exit  change Preview size queue event");
            }
        });
    }

    private int getRotateOrientation() {
        //相机预览buffer的旋转角度。由于Camera获取的buffer为横向图像，将buffer旋转为竖向（即正向竖屏使用手机时，人脸方向朝上）
        int rotateOrientation = VideoStrategyContents.ST_CLOCKWISE_ROTATE_270;

        if (mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotateOrientation = VideoStrategyContents.ST_CLOCKWISE_ROTATE_270;
        } else if (mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK && mCameraProxy.getOrientation() == 90) {
            rotateOrientation = VideoStrategyContents.ST_CLOCKWISE_ROTATE_90;
        } else if (mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK && mCameraProxy.getOrientation() == 270) {
            rotateOrientation = VideoStrategyContents.ST_CLOCKWISE_ROTATE_270;
        }

        return rotateOrientation;
    }

    //----------------------常用api-------------
    /**
     * 是否打开log
     * @return
     */
    private void setEnableLog(boolean isLog){
        LogUtils.setIsLoggable(isLog);
    }
    /**
     * 设置预览宽
     * @return
     */
    private void setPreviewWidth(int previewWidth){
         this.previewWidth = previewWidth;
    }

    /**
     * 设置预览高
     * @return
     */
    private void setPreviewHeight(int previewHeight){
        this.previewHeight = previewHeight;
    }

    /**
     * 设置录制宽
     * @return
     */
    private void setRecordWidth(int recordWidth){
        this.recordWidth = recordWidth;
    }

    /**
     * 设置录制高
     * @return
     */
    private void setRecordHeight(int recordHeight){
        this.recordHeight = recordHeight;
    }

    /**
     * 设置录制帧率
     * @return
     */
    private void setRecordFps(int recordFps){
        this.recordFps = recordFps;
    }

    /**
     * 获取预览分辨率
     * @return
     */
    public float getPreviewFPS() {
        return mFps;
    }

    /**
     * 保存一帧图片
     * @return
     */
    public void setSaveImage(File file) {
        mNeedSave = true;
        this.savaImageFile = file;
    }

    /**
     * 是否激活滤镜
     * @return
     */
    public void enableFilter(boolean isOpenFilter) {
        this.isOpenFilter = isOpenFilter;
    }

    /**
     * 设置滤镜
     * @return
     */
    public void setFilter(BaseVideoFilter baseVideoFilter) {
        mVideoFilter = baseVideoFilter;
    }

    /**
     * 设置每一帧encode时回调
     * @return
     */
    public void setOnFrameDrawListener(OnFrameDrawListener listener) {
        this.onFrameDrawListener = listener;
    }

    public interface OnFrameDrawListener {
        /**
         * 每一帧encode时回调
         *
         * @param encodeFrameCount encode的帧数
         */
        void onFrameDraw(int encodeFrameCount);
    }

    //-----------------录制api----------
    /**
     * 开始录制
     */
    private MediaMuxerWrapper mMuxer;
    private boolean isRecord = false;
    public void startRecord(){
            if(recordWidth <= 0 || recordHeight <=0){
                return;
            }
            try {
                mMuxer = new MediaMuxerWrapper(".mp4");    // if you record audio only, ".m4a" is also OK.
                new MediaVideoEncoder(mMuxer, mMediaEncoderListener, recordWidth, recordHeight,recordFps);
                new MediaAudioEncoder(mMuxer, mMediaEncoderListener);

                mMuxer.prepare();
                mMuxer.startRecording();
                isRecord = true;
            } catch (IOException e) {
                isRecord = false;
                e.printStackTrace();
            }

    }
    /**
     * 停止录制
     */
    public String stopRecord() {
        if (mMuxer != null) {
            String path = mMuxer.getFilePath();
            mMuxer.stopRecording();
            mMuxer = null;
            System.gc();
            return path;
        }
        System.gc();
        isRecord = false;
        return null;
    }

    /**
     * 是否在录制
     */
    public boolean isRecord() {
        return isRecord;
    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder )
                setVideoEncoder((MediaVideoEncoder) encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder )
                setVideoEncoder(null);
        }
    };

    public void setVideoEncoder(final MediaVideoEncoder encoder) {

        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (encoder != null && mVideoEncoderTexture != null) {
                        encoder.setEglContext(EGL14.eglGetCurrentContext(), mVideoEncoderTexture[0]);
                    }
                    mVideoEncoder = encoder;
                }
            }
        });
    }

    private void processStMatrix(float[] matrix, boolean needMirror) {
        if (needMirror && matrix != null && matrix.length == 16) {
            for (int i = 0; i < 3; i++) {
                matrix[4 * i] = -matrix[4 * i];
            }

            if (matrix[4 * 3] == 0) {
                matrix[4 * 3] = 1.0f;
            } else if (matrix[4 * 3] == 1.0f) {
                matrix[4 * 3] = 0f;
            }
        }

        return;
    }
}