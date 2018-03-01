package com.wangshuo.videostrategy.videostrategy.strategy.filter;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.wangshuo.videostrategy.videostrategy.strategy.filter.basefilter.OriginalVideoFilter;


/**
 * Created by wanglei on 2018/2/5.
 */

public class LookUpFilter extends OriginalVideoFilter {

    int intensityHandle;
    int lookupTexHandle;
    float intensity = 1.0f;

    int[] lookupTextures;
    Bitmap lookupBmp;

    static final String FRAG_SHADER = "varying highp vec2 vCamTextureCoord;\n uniform sampler2D inputImageTexture;\n uniform sampler2D inputImageTexture2; // lookup texture\n \n uniform lowp float intensity;\nconst highp float midP=0.0009765625;\nconst highp float offsetP=0.123046875;\n \n void main()\n {\n     highp vec4 textureColor = texture2D(inputImageTexture, vCamTextureCoord);\n     \n     highp float blueColor = textureColor.b * 63.0;\n     \n     highp vec2 quad1;\n     quad1.y = floor(floor(blueColor) / 8.0);\n     quad1.x = floor(blueColor) - (quad1.y * 8.0);\n     \n     highp vec2 quad2;\n     quad2.y = floor(ceil(blueColor) / 8.0);\n     quad2.x = ceil(blueColor) - (quad2.y * 8.0);\n     \n     highp vec2 texPos1;\n     texPos1.x = (quad1.x * 0.125) + midP + (offsetP  * textureColor.r);\n     texPos1.y = (quad1.y * 0.125) + midP + (offsetP  * textureColor.g);\n     \n     highp vec2 texPos2;\n     texPos2.x = (quad2.x * 0.125) + midP + (offsetP  * textureColor.r);\n     texPos2.y = (quad2.y * 0.125) + midP + (offsetP  * textureColor.g);\n     \n     lowp vec4 newColor1 = texture2D(inputImageTexture2, texPos1);\n     lowp vec4 newColor2 = texture2D(inputImageTexture2, texPos2);\n     \n     lowp vec4 newColor = mix(newColor1, newColor2, fract(blueColor));\n     gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), intensity);\n }";

    public LookUpFilter() {
        super(null, FRAG_SHADER);
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);

        intensityHandle = GLES20.glGetUniformLocation(glProgram, "intensity");
        lookupTexHandle = GLES20.glGetUniformLocation(glProgram, "inputImageTexture2");

        lookupTextures = new int[1];
        createTexture(lookupTextures);
    }

    @Override
    protected void onPreDraw() {
        super.onPreDraw();

        GLES20.glUniform1f(intensityHandle, intensity);

        if (lookupBmp != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lookupTextures[0]);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, lookupBmp, 0);
            GLES20.glUniform1i(lookupTexHandle, 1);
        }
    }

    public LookUpFilter setLookupBmp(Bitmap lookupBmp) {
        this.lookupBmp = lookupBmp;
        return this;
    }
}
