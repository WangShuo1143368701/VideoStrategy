package com.wangshuo.videostrategy.videostrategy.strategy.filter;

import android.opengl.GLES20;

import com.wangshuo.videostrategy.videostrategy.strategy.filter.basefilter.OriginalVideoFilter;

import java.nio.FloatBuffer;

/**
 * @author wangshuo    "precision highp float;\n"+
 */
public class BeautyFilter extends OriginalVideoFilter {
    public static final String BILATERAL_FRAGMENT_SHADER = "" +
            "precision lowp float;\n" +
            "precision lowp int;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform vec2 singleStepOffset;\n" +
            "uniform int iternum;\n" +
            "uniform float aaCoef;\n" +
            "uniform float mixCoef;\n" +
            "varying lowp vec2 vCamTextureCoord;\n" +
            "const float distanceNormalizationFactor = 4.0;\n" +
            "const mat3 saturateMatrix = mat3(1.1102,-0.0598,-0.061,-0.0774,1.0826,-0.1186,-0.0228,-0.0228,1.1772);\n" +
            "void main() {\n" +
            "vec2 blurCoord1s[14];\n" +
            "blurCoord1s[0] = vCamTextureCoord + singleStepOffset * vec2( 0.0, -10.0);\n" +
            "blurCoord1s[1] = vCamTextureCoord + singleStepOffset * vec2( 8.0, -5.0);\n" +
            "blurCoord1s[2] = vCamTextureCoord + singleStepOffset * vec2( 8.0, 5.0);\n" +
            "blurCoord1s[3] = vCamTextureCoord + singleStepOffset * vec2( 0.0, 10.0);\n" +
            "blurCoord1s[4] = vCamTextureCoord + singleStepOffset * vec2( -8.0, 5.0);\n" +
            "blurCoord1s[5] = vCamTextureCoord + singleStepOffset * vec2( -8.0, -5.0);\n" +
            "blurCoord1s[6] = vCamTextureCoord + singleStepOffset * vec2( 0.0, -6.0);\n" +
            "blurCoord1s[7] = vCamTextureCoord + singleStepOffset * vec2( -4.0, -4.0);\n" +
            "blurCoord1s[8] = vCamTextureCoord + singleStepOffset * vec2( -6.0, 0.0);\n" +
            "blurCoord1s[9] = vCamTextureCoord + singleStepOffset * vec2( -4.0, 4.0);\n" +
            "blurCoord1s[10] = vCamTextureCoord + singleStepOffset * vec2( 0.0, 6.0);\n" +
            "blurCoord1s[11] = vCamTextureCoord + singleStepOffset * vec2( 4.0, 4.0);\n" +
            "blurCoord1s[12] = vCamTextureCoord + singleStepOffset * vec2( 6.0, 0.0);\n" +
            "blurCoord1s[13] = vCamTextureCoord + singleStepOffset * vec2( 4.0, -4.0);\n" +

            "vec3 centralColor;\n" +
            "float central;\n" +
            "float gaussianWeightTotal;\n" +
            "float sum;\n" +
            "float sampleColor;\n" +
            "float distanceFromCentralColor;\n" +
            "float gaussianWeight;\n" +

            "central = texture2D( inputImageTexture, vCamTextureCoord ).g;\n" +
            "gaussianWeightTotal = 0.2;\n" +
            "sum = central * 0.2;\n" +

            "for (int i = 0; i < 6; i++) {\n" +
            "sampleColor = texture2D( inputImageTexture, blurCoord1s[i] ).g;\n" +
            "distanceFromCentralColor = min( abs( central - sampleColor ) * distanceNormalizationFactor, 1.0 );\n" +
            "gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);\n" +
            "gaussianWeightTotal += gaussianWeight;\n" +
            "sum += sampleColor * gaussianWeight;\n" +
            "}\n" +
            "for (int i = 6; i < 14; i++) {\n" +
            "sampleColor = texture2D( inputImageTexture, blurCoord1s[i] ).g;\n" +
            "distanceFromCentralColor = min( abs( central - sampleColor ) * distanceNormalizationFactor, 1.0 );\n" +
            "gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);\n" +
            "gaussianWeightTotal += gaussianWeight;\n" +
            "sum += sampleColor * gaussianWeight;\n" +
            "}\n" +

            "sum = sum / gaussianWeightTotal;\n" +

            "centralColor = texture2D( inputImageTexture, vCamTextureCoord ).rgb;\n" +
            "sampleColor = centralColor.g - sum + 0.5;\n" +
            "for (int i = 0; i < iternum; ++i) {\n" +
            "if (sampleColor <= 0.5) {\n" +
            "sampleColor = sampleColor * sampleColor * 2.0;\n" +
            "}else {\n" +
            "sampleColor = 1.0 - ((1.0 - sampleColor)*(1.0 - sampleColor) * 2.0);\n" +
            "}\n" +
            "}\n" +
            "float aa = 1.0 + pow( centralColor.g, 0.3 )*aaCoef;\n" +
            "vec3 smoothColor = centralColor*aa - vec3( sampleColor )*(aa - 1.0);\n" +
            "smoothColor = clamp( smoothColor, vec3( 0.0 ), vec3( 1.0 ) );\n" +
            "smoothColor = mix( centralColor, smoothColor, pow( centralColor.g, 0.33 ) );\n" +
            "smoothColor = mix( centralColor, smoothColor, pow( centralColor.g, mixCoef ) );\n" +
            "gl_FragColor = vec4( pow( smoothColor, vec3( 0.96 ) ), 1.0 );\n" +
            "vec3 satcolor = gl_FragColor.rgb * saturateMatrix;\n" +
            "gl_FragColor.rgb = mix( gl_FragColor.rgb, satcolor, 0.23 );\n" +
            "}";

    private int b;
    private int c;
    private int d;
    private float e;
    private float f;
    private int g;
    private int h;


    public BeautyFilter() {
        super(null, BILATERAL_FRAGMENT_SHADER);
        this.g = 4;
        this.e = 0.13F;
        this.f = 0.54F;
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        this.h = GLES20.glGetUniformLocation(glProgram, "singleStepOffset");
        this.b = GLES20.glGetUniformLocation(glProgram, "aaCoef");
        this.c = GLES20.glGetUniformLocation(glProgram, "mixCoef");
        this.d = GLES20.glGetUniformLocation(glProgram, "iternum");

        onOutputSizeChanged(VWidth, VHeight);
        a(g, e, f);
    }


    public void onOutputSizeChanged(int width, int height) {
        float var10001 = (float) width;
        float var5 = (float) height;
        float var4 = var10001;
        GLES20.glUniform2fv(this.h, 1, FloatBuffer.wrap(new float[]{2.0F / var4, 2.0F / var5}));
    }

    public void setFilterLevel(float level1, float level2, float level3) {
        this.setFilterLevel(level1);
    }

    public void setFilterLevel(float flag) {
        switch ((int) (flag / 20.0F + 1.0F)) {
            case 1:
                this.a(1, 0.19F, 0.54F);
                return;
            case 2:
                this.a(2, 0.29F, 0.54F);
                return;
            case 3:
                this.a(3, 0.17F, 0.39F);
                return;
            case 4:
                this.a(3, 0.25F, 0.54F);
                return;
            case 5:
                this.a(4, 0.13F, 0.54F);
                return;
            case 6:
                this.a(4, 0.19F, 0.69F);
                return;
            default:
                this.a(0, 0.0F, 0.0F);
        }
    }

    private void a(int var1, float var2, float var3) {
        this.g = var1;
        this.e = var2;
        this.f = var3;
    }

    @Override
    protected void onPreDraw() {
        super.onPreDraw();
        GLES20.glUniform1i(this.d, this.g);
        GLES20.glUniform1f(this.b, this.e);
        GLES20.glUniform1f(this.c, this.f);
    }
}
