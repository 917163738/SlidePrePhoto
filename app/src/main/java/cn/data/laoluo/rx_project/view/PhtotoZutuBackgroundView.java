package cn.data.laoluo.rx_project.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by flame on 2017/5/5.
 */

public class PhtotoZutuBackgroundView extends LinearLayout {
    public PhtotoZutuBackgroundView(Context context) {
        super(context);
    }

    public PhtotoZutuBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
    }


}
