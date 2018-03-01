package com.wangshuo.videostrategy.videostrategy.strategy.utils;

/**
 * Created by wangshuo on 2017/12/19.
 */

public class VideoStrategyContents {

    public static final int MSG_SAVING_IMG = 1;

    public final static int ST_CLOCKWISE_ROTATE_0 = 0;  //< 图像不需要转向
    public final static int ST_CLOCKWISE_ROTATE_90 = 1;  //< 图像需要顺时针旋转90度
    public final static int ST_CLOCKWISE_ROTATE_180 = 2; //< 图像需要顺时针旋转180度
    public final static int ST_CLOCKWISE_ROTATE_270 = 3; //< 图像需要顺时针旋转270度
}
