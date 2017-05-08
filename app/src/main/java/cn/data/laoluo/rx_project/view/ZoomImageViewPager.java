package cn.data.laoluo.rx_project.view;

import android.content.Context;
import android.os.SystemClock;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;


/**
 * 可以支持图片放大缩小的viewpager，需要配合zoomimage一起实现
 */
public class ZoomImageViewPager extends ViewPager {
    private ZoomImageView mImgView = null;
	private float mLastX;
	private final int mTouchSlop;
	private float mPerformDownX;
	private float mPerformDownY;
    private long mPerformDownTime;
    private int mScolledPosition = 0;
    private int mScolledOffsetPixels = 0;
    private int mPageWidth = 0;

    public ZoomImageViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                mScolledPosition = position;
                mScolledOffsetPixels = positionOffsetPixels;
//                LogHelper.d("zoomview", "onPageScrolled position, positionOffset, positionOffsetPixels = "
//                        + position + ", " + positionOffset + ", " + positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //1,手动拖动状态
                //2，放手回原位的过程中
                //0，静止状态
                if (state == 0) {
                    mScolledPosition = getCurrentItem();
                    mScolledOffsetPixels = 0;
                }
            }
        });
	}

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mPageWidth = getWidth();
    }
	
	public void setZoomImageView(ZoomImageView view) {
		if (view == null) {
			throw new RuntimeException("setZoomImageView view == null!");
		}
		mImgView = view;
	}

    public ZoomImageView getImgView() {
        return mImgView;
    }

	@Override
	public boolean onTouchEvent(MotionEvent event) {
        boolean handle = true;
	    try {
            //判断该viewpager是应该响应自己的左右滑动，还是应该判断是否要调用mImgView.hanleTouchEvent来处理
	    	if (mImgView == null) {
		        return super.onTouchEvent(event);
		    }
			int actionMasked = event.getActionMasked();
            switch (actionMasked) {
                case MotionEvent.ACTION_DOWN:
                    //模拟一个onClick事件
                    mLastX = event.getX();
                    mPerformDownX = mLastX;
                    mPerformDownY = event.getY();
                    mPerformDownTime = SystemClock.uptimeMillis();
                    break;
                case MotionEvent.ACTION_MOVE:
                    //由viewpager滑动转向图片拖动的过程
                    float x = event.getX();
                    if (mImgView.canHorizontalDrag() && mImgView.getTouchMode() == ZoomImageView.NONE) {
                        float deltaX = x - mLastX;

                        mLastX = x;
                        int currentPosition = getCurrentItem();
                        //viewpager滑动到下一页是currentposion,0 到 currentposion,1079的过程
                        //viewpager滑动到上一页是currentposion - ,1079 到 currentposion - 1,0的过程

                        //由滑动下一页转变为滑动上一页的过程，并且图片在右边缘，所以此时应该把viewpager滚动到当前页，
                        //并且开始拖动图片的过程
                        if (mScolledPosition == currentPosition && mScolledOffsetPixels - deltaX < 3
                                && mImgView.isOnRightSide()) {
                            //由滑动下一页转变为滑动上一页的过程，并且图片在右边缘，所以此时应该把viewpager滚动到当前页，
                            //并且开始拖动图片的过程
                            scrollBy(-mScolledOffsetPixels, 0);
                            mScolledOffsetPixels = 0;
                            mScolledPosition = currentPosition;
                            event.setAction(MotionEvent.ACTION_DOWN);
                        } else if (mScolledPosition == currentPosition-1 && mScolledOffsetPixels - deltaX > (mPageWidth-3)
                                && mImgView.isOnLeftSide()) {
                            //由滑动上一页转变为滑动下一页的过程，并且图片在左边缘，所以此时应该把viewpager滚动到当前页，
                            //并且开始拖动图片的过程
                            scrollBy(mPageWidth - mScolledOffsetPixels, 0);
                            mScolledOffsetPixels = 0;
                            mScolledPosition = currentPosition;
                            event.setAction(MotionEvent.ACTION_DOWN);
                        }
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (mScolledOffsetPixels == 0) {
                        mImgView.setCanZoom(true);
                    } else {
                        mImgView.setCanZoom(false);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    float upx = event.getX();
                    float upy = event.getY();
                    if (mScolledOffsetPixels == 0 && mOnClikListener != null
                            && Math.abs(upx - mPerformDownX) < mTouchSlop * 2
                            && Math.abs(upy - mPerformDownY) <= mTouchSlop * 2
                            && (SystemClock.uptimeMillis() - mPerformDownTime) < 300) {
                        mOnClikListener.onClick(mImgView);
                    }
                    break;
                default:
                    break;
            }

//            LogHelper.d("zoomview", "onPageScrollStateChanged mScolledOffsetPixels = " + mScolledOffsetPixels);
            int imgMode = mImgView.hanleTouchEvent(event, mScolledOffsetPixels == 0);
            if (imgMode != ZoomImageView.NONE) {
                boolean notInEdge = event.getActionMasked() == MotionEvent.ACTION_UP && Math.abs(getScrollX()/mPageWidth) > 5;
                if (!notInEdge) {
                    return true;
                }
            }
            handle = super.onTouchEvent(event);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return handle;
	}

    private OnClickListener mOnClikListener;

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnClikListener = l;
    }
}
