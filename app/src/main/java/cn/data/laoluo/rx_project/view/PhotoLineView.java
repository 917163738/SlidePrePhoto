package cn.data.laoluo.rx_project.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

/**
 * Created by flame on 2017/5/5.
 */

public class PhotoLineView extends View {
    public PhotoLineView(Context context) {
        super(context);
    }

    public PhotoLineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    private int mLineWidth = 0;
    private int mOriginHeight, mOriginWidth;

    /**
     * 设置背景大图的显示区域尺寸，用来计算黄圈的宽度
     * @param width
     * @param height
     */
    public void setOriginSize(int width, int height) {
        mOriginHeight = height;
        mOriginWidth = width;
    }

    /**
     * 根据手机尺寸与图片宽带动态设置黄圈宽度
     *
     * @param imgWidth
     */
    public void setLineWidth(int imgWidth) {
//        DisplayMetrics dm = getResources().getDisplayMetrics();
//        int dWidth = dm.widthPixels * getHeight() / 2560;
//        int dWidth = dm.widthPixels * getHeight() / dm.heightPixels;
        int dWidth = mOriginWidth * getHeight() / mOriginHeight;


        Log.e("swc", "dWidth:" + dWidth);
        if (imgWidth < dWidth) {
            mLineWidth = imgWidth;
        } else {
            mLineWidth = dWidth;
        }
        Log.e("swc", "mLineWidth:" + mLineWidth);
        postInvalidate();

    }

    public int getLineWidth() {
        return mLineWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 设置画布的背景颜色
        canvas.drawColor(Color.TRANSPARENT);
        /**
         * 定义矩形为空心
         */
        // 定义画笔1
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        // 消除锯齿
        paint.setAntiAlias(true);
        // 设置画笔的颜色
        paint.setColor(Color.YELLOW);
        // 设置paint的外框宽度
        paint.setStrokeWidth(4);
        // 画一个长方形
        canvas.drawRect(0, 0, mLineWidth, getHeight(), paint);
    }
}
