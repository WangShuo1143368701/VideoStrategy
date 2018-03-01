package com.wangshuo.videostrategy.videostrategy.strategy.filter.basefilter;

import android.opengl.GLES20;

import com.wangshuo.videostrategy.videostrategy.strategy.filter.tools.GLHelper;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class BaseVideoFilter {
    protected int SIZE_WIDTH;
    protected int SIZE_HEIGHT;
    protected int directionFlag = -1;
    protected ShortBuffer drawIndecesBuffer;

    public void onInit(int VWidth, int VHeight) {
        SIZE_WIDTH = VWidth;
        SIZE_HEIGHT = VHeight;
        drawIndecesBuffer = GLHelper.getDrawIndecesBuffer();
    }

    public void onDraw(final int cameraTexture, final int targetFrameBuffer, final FloatBuffer shapeBuffer, final FloatBuffer textrueBuffer) {
    }

    public void onDestroy() {

    }

    public void onDirectionUpdate(int _directionFlag) {
        this.directionFlag = _directionFlag;
    }

    protected void setFloat(final int location, final float floatValue) {
        GLES20.glUniform1f(location, floatValue);
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
    }

    protected void createTexture(int[] textures) {
        GLES20.glGenTextures(textures.length, textures, 0);
        for (int pos = 0; pos < textures.length; pos++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[pos]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
    }
}
