/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wangshuo.videostrategy.videostrategy.strategy.filter;

import android.opengl.GLES20;

import com.wangshuo.videostrategy.videostrategy.strategy.filter.basefilter.OriginalVideoFilter;


/**
 * Created by Wangshuo on 2017/11/13.
 */
public class BrightnessFilter extends OriginalVideoFilter {
    public static final String BRIGHTNESS_FRAGMENT_SHADER = "" +
            "varying lowp vec2 vCamTextureCoord;\n"+
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform lowp float brightness;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     lowp vec4 textureColor = texture2D(inputImageTexture, vCamTextureCoord);\n" +
            "     \n" +
            "     gl_FragColor = vec4((textureColor.rgb + vec3(brightness)), textureColor.w);\n" +
            " }";

    private int mBrightnessLocation;
    private float mBrightness;



    public BrightnessFilter() {
        super(null, BRIGHTNESS_FRAGMENT_SHADER);
        mBrightness = 0.15f;
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth,VHeight);
        mBrightnessLocation = GLES20.glGetUniformLocation(glProgram, "brightness");
    }



    @Override
    protected void onPreDraw() {
        super.onPreDraw();
        setBrightness(mBrightness);
    }

    public void setBrightness(final float brightness) {
        mBrightness = brightness;
        GLES20.glUniform1f(mBrightnessLocation, mBrightness);
    }

    protected static float range(final int percentage, final float start, final float end) {
        return (end - start) * percentage / 100.0f + start;
    }
    //    ((BrightnessFilter)filter).setBrightness(range(progress, 0.0f, 0.5f));
}
