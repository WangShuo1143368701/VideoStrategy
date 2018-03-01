package com.wangshuo.videostrategy.videostrategy;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.wangshuo.videostrategy.videostrategy.strategy.display.CameraDisplayStrategy;
import com.wangshuo.videostrategy.videostrategy.strategy.filter.BeautyFilter;

public class MainActivity extends AppCompatActivity {

    private CameraDisplayStrategy mCameraDisplay;        //双输入
    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StatusBarUtils.setTranslucentStatus(this);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.glView);

        initVideoStrategy();
    }

    private void initVideoStrategy() {
        mCameraDisplay = new CameraDisplayStrategy(getApplicationContext(), mDoubleInputChangePreviewSizeListener, glSurfaceView);
        mCameraDisplay.enableFilter(true);
        mCameraDisplay.setFilter(new BeautyFilter());
        mCameraDisplay.setOnFrameDrawListener(onFrameDrawListener);
    }

    private CameraDisplayStrategy.ChangePreviewSizeListener mDoubleInputChangePreviewSizeListener = new CameraDisplayStrategy.ChangePreviewSizeListener() {
        @Override
        public void onChangePreviewSize(final int previewW, final int previewH) {

        }
    };

    private CameraDisplayStrategy.OnFrameDrawListener onFrameDrawListener = new CameraDisplayStrategy.OnFrameDrawListener() {
        @Override
        public void onFrameDraw(final int encodeFrameCount) {


        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mCameraDisplay.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraDisplay.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraDisplay.onDestroy();
    }
}
