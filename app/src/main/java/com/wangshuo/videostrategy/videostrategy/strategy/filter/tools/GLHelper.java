package com.wangshuo.videostrategy.videostrategy.strategy.filter.tools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class GLHelper {
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    private static String VERTEXSHADER = "" +
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main(){\n" +
            "    gl_Position= aPosition;\n" +
            "    vTextureCoord = aTextureCoord;\n" +
            "}";
    private static final String VERTEXSHADER_CAMERA2D =
            "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "uniform mat4 uTextureMatrix;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main(){\n" +
                    "    gl_Position= aPosition;\n" +
                    "    vTextureCoord = (uTextureMatrix * aTextureCoord).xy;\n" +
                    "}";
    private static String FRAGMENTSHADER_CAMERA = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main(){\n" +
            "    vec4  color = texture2D(uTexture, vTextureCoord);\n" +
            "    gl_FragColor = color;\n" +
            "}";
    private static String FRAGMENTSHADER_CAMERA2D = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES uTexture;\n" +
            "void main(){\n" +
            "    vec4  color = texture2D(uTexture, vTextureCoord);\n" +
            "    gl_FragColor = color;\n" +
            "}";
    private static String FRAGMENTSHADER_2D = "" +
            "precision highp float;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main(){\n" +
            "    vec4  color = texture2D(uTexture, vTextureCoord);\n" +
            "    gl_FragColor = color;\n" +
            "}";
    private static short drawIndices[] = {0, 1, 2, 0, 2, 3};
    private static float SquareVertices[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f};
    private static float CamTextureVertices[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f};
    private static float Cam2dTextureVertices[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f};


    public static int FLOAT_SIZE_BYTES = 4;
    public static int SHORT_SIZE_BYTES = 2;


    public static ShortBuffer getDrawIndecesBuffer() {
        ShortBuffer result = ByteBuffer.allocateDirect(SHORT_SIZE_BYTES * drawIndices.length).
                order(ByteOrder.nativeOrder()).
                asShortBuffer();
        result.put(drawIndices);
        result.position(0);
        return result;
    }

    public static FloatBuffer getShapeVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * SquareVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(SquareVertices);
        result.position(0);
        return result;
    }


    public static FloatBuffer getCameraTextureVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * Cam2dTextureVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(CamTextureVertices);
        result.position(0);
        return result;
    }

}