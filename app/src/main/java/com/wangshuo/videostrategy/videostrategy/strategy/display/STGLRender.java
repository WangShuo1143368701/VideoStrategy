package com.wangshuo.videostrategy.videostrategy.strategy.display;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.wangshuo.videostrategy.videostrategy.strategy.filter.basefilter.BaseVideoFilter;
import com.wangshuo.videostrategy.videostrategy.strategy.filter.tools.GLHelper;
import com.wangshuo.videostrategy.videostrategy.strategy.glutils.GlUtil;
import com.wangshuo.videostrategy.videostrategy.strategy.glutils.OpenGLUtils;
import com.wangshuo.videostrategy.videostrategy.strategy.glutils.TextureRotationUtil;
import com.wangshuo.videostrategy.videostrategy.strategy.utils.LogUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by wangshuo on 17-11-16.
 */

public class STGLRender {

    private final static String TAG = "STGLRender";
    private static final String CAMERA_INPUT_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "	textureCoordinate = inputTextureCoordinate.xy;\n" +
            "	gl_Position = position;\n" +
            "}";

    private static final String CAMERA_INPUT_FRAGMENT_SHADER_OES = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "\n" +
            "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "	gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    public static final String CAMERA_INPUT_FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    public static final String DRAW_POINTS_VERTEX_SHADER = "" +
            "attribute vec4 aPosition;\n" +
            "void main() {\n" +
            "  gl_PointSize = 2.0;" +
            "  gl_Position = aPosition;\n" +
            "}";

    public static final String DRAW_POINTS_FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "uniform vec4 uColor;\n" +
            "void main() {\n" +
            "  gl_FragColor = uColor;\n" +
            "}";

    //
    private final static String DRAW_POINTS_PROGRAM = "mPointProgram";
    private final static String DRAW_POINTS_COLOR = "uColor";
    private final static String DRAW_POINTS_POSITION = "aPosition";
    private int mDrawPointsProgram = 0;
    private int mColor = -1;
    private int mPosition = -1;
    private int[] mPointsFrameBuffers;

    private final static String PROGRAM_ID = "program";
    private final static String POSITION_COORDINATE = "position";
    private final static String TEXTURE_UNIFORM = "inputImageTexture";
    private final static String TEXTURE_COORDINATE = "inputTextureCoordinate";
    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private final FloatBuffer mGLSaveTextureBuffer;

    private FloatBuffer mTextureBuffer;
    private FloatBuffer mVertexBuffer;

    private boolean mIsInitialized;
    private ArrayList<HashMap<String, Integer>> mArrayPrograms = new ArrayList<HashMap<String, Integer>>(2) {
        {
            for (int i = 0; i < 2; ++i) {
                HashMap<String, Integer> hashMap = new HashMap<>();
                hashMap.put(PROGRAM_ID, 0);
                hashMap.put(POSITION_COORDINATE, -1);
                hashMap.put(TEXTURE_UNIFORM, -1);
                hashMap.put(TEXTURE_COORDINATE, -1);
                add(hashMap);
            }
        }
    };
    private int mViewPortWidth;
    private int mViewPortHeight;
    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;
    private int[] mFilterBuffers;
    private int[] mFilterTextures;
    private FloatBuffer mFilterVertexBuffer;
    private FloatBuffer mFilterTextureBuffer;
    private BaseVideoFilter mBaseVideoFilter;


    public STGLRender() {
        mGLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

        mGLSaveTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLSaveTextureBuffer.put(TextureRotationUtil.getRotation(0, false, true)).position(0);
        mFilterVertexBuffer = GLHelper.getShapeVerticesBuffer();
        mFilterTextureBuffer = GLHelper.getCameraTextureVerticesBuffer();
    }

    public void init(int width, int height) {
        if (mViewPortWidth == width && mViewPortHeight == height) {
            return;
        }
        initProgram(CAMERA_INPUT_FRAGMENT_SHADER_OES, mArrayPrograms.get(0));
        initProgram(CAMERA_INPUT_FRAGMENT_SHADER, mArrayPrograms.get(1));
        mViewPortWidth = width;
        mViewPortHeight = height;
        initFrameBuffers(width, height);
        mIsInitialized = true;
    }

    private void initProgram(String fragment, HashMap<String, Integer> programInfo) {
        int proID = programInfo.get(PROGRAM_ID);
        if (proID == 0) {
            proID = OpenGLUtils.loadProgram(CAMERA_INPUT_VERTEX_SHADER, fragment);
            programInfo.put(PROGRAM_ID, proID);
            programInfo.put(POSITION_COORDINATE, GLES20.glGetAttribLocation(proID, POSITION_COORDINATE));
            programInfo.put(TEXTURE_UNIFORM, GLES20.glGetUniformLocation(proID, TEXTURE_UNIFORM));
            programInfo.put(TEXTURE_COORDINATE, GLES20.glGetAttribLocation(proID, TEXTURE_COORDINATE));
        }
    }

    public void initDrawPoints() {
        mDrawPointsProgram = OpenGLUtils.loadProgram(DRAW_POINTS_VERTEX_SHADER, DRAW_POINTS_FRAGMENT_SHADER);
        mColor = GLES20.glGetAttribLocation(mDrawPointsProgram, DRAW_POINTS_POSITION);
        mPosition = GLES20.glGetUniformLocation(mDrawPointsProgram, DRAW_POINTS_COLOR);

        if (mPointsFrameBuffers == null) {
            mPointsFrameBuffers = new int[1];

            GLES20.glGenFramebuffers(1, mPointsFrameBuffers, 0);
        }
    }

    public void adjustTextureBuffer(int orientation, boolean flipVertical) {
        float[] textureCords = TextureRotationUtil.getRotation(orientation, true, flipVertical);
        LogUtils.d(TAG, "==========rotation: " + orientation + " flipVertical: " + flipVertical
                + " texturePos: " + Arrays.toString(textureCords));
        if (mTextureBuffer == null) {
            mTextureBuffer = ByteBuffer.allocateDirect(textureCords.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mTextureBuffer.clear();
        mTextureBuffer.put(textureCords).position(0);
    }

    /**
     * 用来计算贴纸渲染的纹理最终需要的顶点坐标
     */
    public void calculateVertexBuffer(int displayW, int displayH, int imageW, int imageH) {
        int outputHeight = displayH;
        int outputWidth = displayW;

        float ratio1 = (float) outputWidth / imageW;
        float ratio2 = (float) outputHeight / imageH;
        float ratioMin = Math.min(ratio1, ratio2);
        int imageWidthNew = Math.round(imageW * ratioMin);
        int imageHeightNew = Math.round(imageH * ratioMin);

        float ratioWidth = imageWidthNew / (float) outputWidth;
        float ratioHeight = imageHeightNew / (float) outputHeight;

        float[] cube = new float[]{
                TextureRotationUtil.CUBE[0] / ratioHeight, TextureRotationUtil.CUBE[1] / ratioWidth,
                TextureRotationUtil.CUBE[2] / ratioHeight, TextureRotationUtil.CUBE[3] / ratioWidth,
                TextureRotationUtil.CUBE[4] / ratioHeight, TextureRotationUtil.CUBE[5] / ratioWidth,
                TextureRotationUtil.CUBE[6] / ratioHeight, TextureRotationUtil.CUBE[7] / ratioWidth,
        };

        if (mVertexBuffer == null) {
            mVertexBuffer = ByteBuffer.allocateDirect(cube.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mVertexBuffer.clear();
        mVertexBuffer.put(cube).position(0);
    }

    /**
     * 此函数有三个功能
     * 1. 将OES的纹理转换为标准的GL_TEXTURE_2D格式
     * 2. 将纹理宽高对换，即将wxh的纹理转换为了hxw的纹理，并且如果是前置摄像头，则需要有水平的翻转
     * 3. 读取上面两个步骤后纹理的内容到cpu内存，存储为RGBA格式的buffer
     *
     * @param textureId 输入的OES的纹理id
     * @param buffer    输出的RGBA的buffer
     * @return 转换后的GL_TEXTURE_2D的纹理id
     */
    public int preProcess(int textureId, ByteBuffer buffer) {
        if (mFrameBuffers == null
                || !mIsInitialized)
            return -2;

        GLES20.glUseProgram(mArrayPrograms.get(0).get(PROGRAM_ID));
        GlUtil.checkGlError("glUseProgram");

        mGLCubeBuffer.position(0);
        int glAttribPosition = mArrayPrograms.get(0).get(POSITION_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);

        mTextureBuffer.position(0);
        int glAttribTextureCoordinate = mArrayPrograms.get(0).get(TEXTURE_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);

        if (textureId != -1) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mArrayPrograms.get(0).get(TEXTURE_UNIFORM), 0);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GlUtil.checkGlError("glBindFramebuffer");
        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        if (buffer != null) {
            GLES20.glReadPixels(0, 0, mViewPortWidth, mViewPortHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        }

        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glUseProgram(0);

        return mFrameBufferTextures[0];
    }

    public void destroyFrameBuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(2, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(2, mFrameBuffers, 0);
            mFrameBuffers = null;
        }

        if (mPointsFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mPointsFrameBuffers, 0);
            mPointsFrameBuffers = null;
        }

        if (mFilterBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFilterBuffers, 0);
            mFilterBuffers = null;
        }
        if(mBaseVideoFilter != null){
            mBaseVideoFilter.onDestroy();
            mBaseVideoFilter = null;
        }

    }

    public void onDrawPoints(int textureId, float[] points) {

        if (mDrawPointsProgram == 0) {
            initDrawPoints();
        }

        GLES20.glUseProgram(mDrawPointsProgram);
        GLES20.glUniform4f(mColor, 0.0f, 1.0f, 0.0f, 1.0f);

        FloatBuffer buff = null;

        buff = ByteBuffer.allocateDirect(points.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        buff.clear();
        buff.put(points).position(0);

        GLES20.glVertexAttribPointer(mPosition, 2, GLES20.GL_FLOAT, false, 0, buff);
        GLES20.glEnableVertexAttribArray(mPosition);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mPointsFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);

        GlUtil.checkGlError("glBindFramebuffer");
        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, points.length / 2);

        GLES20.glDisableVertexAttribArray(mPosition);

    }

    public int onDrawFrame(final int textureId) {

        if (!mIsInitialized) {
            return OpenGLUtils.NOT_INIT;
        }

        GLES20.glUseProgram(mArrayPrograms.get(1).get(PROGRAM_ID));

        mVertexBuffer.position(0);
        int glAttribPosition = mArrayPrograms.get(1).get(POSITION_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);

        mGLTextureBuffer.position(0);
        int glAttribTextureCoordinate = mArrayPrograms.get(1).get(TEXTURE_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);

        if (textureId != OpenGLUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mArrayPrograms.get(1).get(TEXTURE_UNIFORM), 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return OpenGLUtils.ON_DRAWN;
    }

    public int saveTextureToFrameBuffer(int textureOutId, ByteBuffer buffer) {
        if (mFrameBuffers == null) {
            return OpenGLUtils.NO_TEXTURE;
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[1]);
        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);

        GLES20.glUseProgram(mArrayPrograms.get(1).get(PROGRAM_ID));

        if (!mIsInitialized) {
            return OpenGLUtils.NOT_INIT;
        }

        mGLCubeBuffer.position(0);
        int glAttribPosition = mArrayPrograms.get(1).get(POSITION_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);

        mGLSaveTextureBuffer.position(0);
        int glAttribTextureCoordinate = mArrayPrograms.get(1).get(TEXTURE_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLSaveTextureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);

        if (textureOutId != OpenGLUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureOutId);
            GLES20.glUniform1i(mArrayPrograms.get(1).get(TEXTURE_UNIFORM), 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        if (buffer != null) {
            GLES20.glReadPixels(0, 0, mViewPortWidth, mViewPortHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        }

        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        return mFrameBufferTextures[1];
    }

    private void initFrameBuffers(int width, int height) {
        destroyFrameBuffers();

        if (mFrameBuffers == null) {
            mFrameBuffers = new int[2];
            mFrameBufferTextures = new int[2];

            GLES20.glGenFramebuffers(2, mFrameBuffers, 0);
            GLES20.glGenTextures(2, mFrameBufferTextures, 0);

            bindFrameBuffer(mFrameBufferTextures[0], mFrameBuffers[0], width, height);
            bindFrameBuffer(mFrameBufferTextures[1], mFrameBuffers[1], width, height);
        }
        if (mFilterBuffers == null) {
            mFilterBuffers = new int[1];
            mFilterTextures = new int[1];
            GLES20.glGenFramebuffers(1, mFilterBuffers, 0);
            GLES20.glGenTextures(1, mFilterTextures, 0);
            bindFrameBuffer(mFilterTextures[0], mFilterBuffers[0], width, height);
        }
    }

    private void bindFrameBuffer(int textureId, int frameBuffer, int width, int height) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public final void destroy() {
        mIsInitialized = false;
        destroyFrameBuffers();
        GLES20.glDeleteProgram(mArrayPrograms.get(0).get(PROGRAM_ID));
        GLES20.glDeleteProgram(mArrayPrograms.get(1).get(PROGRAM_ID));
    }

    public int drawFilterBuffer(final int textureId, BaseVideoFilter mVideoFilter) {
            if(mVideoFilter != null){
                if (mVideoFilter != mBaseVideoFilter) {
                    if (mBaseVideoFilter != null) {
                        mBaseVideoFilter.onDestroy();
                    }
                    mBaseVideoFilter = mVideoFilter;
                    if (mBaseVideoFilter != null) {
                        mBaseVideoFilter.onInit(mViewPortWidth, mViewPortHeight);
                    }
                }
                if(mBaseVideoFilter != null){
                    synchronized (this) {
                        //mBaseVideoFilter.onDirectionUpdate(90);
                        mBaseVideoFilter.onDraw(textureId, mFilterBuffers[0], mFilterVertexBuffer, mFilterTextureBuffer);
                    }
                }
                return mFilterTextures[0];
            }

        return textureId;
    }


}
