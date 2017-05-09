package cn.data.laoluo.rx_project.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Scroller;

import cn.data.laoluo.rx_project.R;

public class ZoomImageView extends ImageView {
    private float mLastX;
    private float mLastY;
    private Matrix mMatrix = new Matrix();

    // 本来bitmap的宽高是不规则的，初始化后scale参数也是一个不规则的数字，为了后面计算的方便，
    // 我们可以规定初始化后的scale为1.0，bitmap宽高为初始化后的宽高，计算缩放等是用的matrix.postScale
    // 是一个缩放的增量系数
    private float mCurrentScale = 1.0f;

    //如果图片被双击，会在最小尺寸和最大尺寸之间切换
    private float mMatrixX, mMatrixY;

    private float mOldDistant;
    private float mCurrentDistant;
    private PointF mCenter = new PointF();

    private float mZoomMinScale = 0.1f;
    private float mZommMaxScale = 3.5f;

    //放大缩放超过或小于这值后自动回到此scale
    private float mBackMaxScale, mBackMinScale;
    public static final int NONE = 0;
    public static final int DRAG = 1;
    public static final int ZOOM = 2;
    public static final int ZOOM_ANIM = 3;

    private float[] mTmpArray = new float[9];
    private static TimeInterpolator sInterpolator = new DecelerateInterpolator();
    private AnimatorUpdateListener zoomBacklistener = null;

    //原始bitmap的宽高，和 初始化填充完后的宽高，
    private float mOriBmpWidth = 0, mOriBmpHeight = 0, mBmpWidth = 0,
            mBmpHeight = 0, mWidth, mHeight = 0;

    //x，y方向的剪裁框内的冗余量，用剪裁框的宽度减去当前图片的宽度，负数说明图片大于剪裁框
    private float mRedundantXSpace, mRedundantYSpace;
    private int mMode;
    private boolean mAlreadyLoadBigBmp = false;
    private boolean mCanZoom = true;
    private boolean mIsOnLeftSide, mIsOnRightSide, mIsOnTopSide, mIsOnBottomSide;
    private int mPointCount;
    private Bitmap mBitmap = null;

    /**
     * 这个imagview的父容器是否可以左右滚动，也就是是否是配合viewpager使用的，如果在viewpager中使用，
     * 那么这个值为true
     */
    private boolean mParentCanScroll = false;

    /**
     * 图片初次显示时，要填满的区域（大部分情况其实是填满屏幕，但有时不是，
     * 比如截取图片用来做锁屏图片，很多时候是在屏幕内有个模拟手机样式的剪裁框来铺满图片，
     * 所以不能和屏幕边缘等同），比如系统默认的就是此view的宽高减去padding值，
     * 而我们是自己用matrix填充的bitmap，所以要有一个填充图片的区域
     */
    private RectF mFirstFillRect;

    /**
     * 图片允许的最小方框，比如在图片拖动时，图片边缘不能进入的区域，图片缩小后自动回弹时，
     * 要回弹的那个区域，（ps:这个最小区域，很多时候会和mFirstFillRect区域重合，但有时不同，
     * 如用来截取头像功能时，初始时会填满屏幕，在屏幕中最有一个小一些的截图框，所以这个框也可以
     * 用来获取当截图区域，获取截取的图像）
     */
    private RectF mMinLimitRect;

    /**
     * 图片缩放到小于mMinLimitRect时需要回弹到充满mMinLimitRect，也就是图片充满mMinLimitRect时对应的缩放系数
     */
    private float mMinScale = 1.0f;

    /**
     * 图片的填充是自己实现的，一种是窄边顶满屏幕，一种是长边顶满屏幕
     * 1，窄边充满，长边剪裁掉
     * 2，长边充满，窄边处留白, (留白的区域不能进入mMinLimitRect内)
     */
    private int mScaleType = 1;

//    /**
//     * 是否支持剪裁功能
//     */
//    private boolean mHasClip = false;

    /**
     * 判断用户是否开始滑动的，手指防抖裕量
     */
    private int mTouchSlop;

    private float mScrollDistance = 0;

    /**
     * 速度追踪器
     */
    private VelocityTracker mVelocityTracker;
    private FlingRunnable mFlingRunnable;
    public ZoomImageView(Context context) {
        super(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ZoomImageView, defStyleAttr, 0);
        mScaleType = a.getInt(R.styleable.ZoomImageView_ziv_scaleType, 1);
        a.recycle();
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public void setScaleTypeInt(int scaleType) {
        mScaleType = scaleType;
    }

    /**
     * 设置初始填充框
     */
    public void setFirstFillRect(RectF edgeRect) {
        mFirstFillRect = edgeRect;
        calcBitmapWH();
    }

    /**
     * 图片靠左还是靠右显示
     */
    private boolean mImageAtLeftOrRight = true;

    /**
     * 自己实现初始时图片怎么显示，这里是用matrix实现的center_crop的效果，
     * 而且可以实现以任意的框来center_crop, 系统的scaleType除了fitXy，其他
     * 的也是这样用matrix变换的，参考系统源码，imageview中的configureBounds()方法
     */
    private void calcBitmapWH() {
        if (mWidth == 0 || mHeight == 0 || mOriBmpWidth == 0
                || mOriBmpHeight == 0) {
            return;
        }

        if (mFirstFillRect == null) {
            mFirstFillRect = new RectF(0, 0, mWidth, mHeight);
        }

        if (mMinLimitRect == null || mMinLimitRect.isEmpty()) {
            mMinLimitRect = new RectF(mFirstFillRect);
        }

        //初始填充框不包含最小框，数据错误，需要纠正初始填充框
        if (!mFirstFillRect.contains(mMinLimitRect)) {
            mFirstFillRect = new RectF(mMinLimitRect);
        }

        //模拟系统的形式来处理图片，参考系统源码，imageview中的configureBounds()方法
        float scale, dx, dy; //scale 代表原图要扩大多少倍
        float scaleX = mFirstFillRect.width() / mOriBmpWidth;
        float scaleY = mFirstFillRect.height() / mOriBmpHeight;
        //静止时允许的最小scale，（相对于原图）
        float minScale = Math.max(mMinLimitRect.width() / mOriBmpWidth, mMinLimitRect.height() / mOriBmpHeight);
        if (mScaleType == 1) { //窄边充满，长边剪裁
            scale = Math.max(scaleX, scaleY);
        } else { //长边充满，窄边缩放留白
            scale = Math.min(scaleX, scaleY);
//            mScaleForDubleClick = Math.max(scaleX, scaleY) / scale;
        }
        //此时有可能窄边缩放后会比最小限制框还要窄，这是不行的，最小的填充后宽高也应该大于等于剪裁框，所以应该特殊处理。
        if (!mMinLimitRect.equals(mFirstFillRect)) {
            if (scale < minScale) {
                scale = minScale;
            }
        }
        mMinScale = minScale / scale;
        mBackMaxScale = scale;
        mBackMinScale = Math.min(mMinLimitRect.width() / mOriBmpWidth, mMinLimitRect.height() / mOriBmpHeight);
        // 本来bitmap的宽高是不规则的，初始化后scale参数也是一个不规则的数字，为了后面计算的方便，
        // 我们可以规定初始化后的scale为1.0，bitmap宽高为初始化后的宽高，计算缩放等是用的matrix.postScale
        // 是一个缩放的增量系数
        mCurrentScale = 1.0f;
        mBackMinScale = mBackMinScale / mBackMaxScale;
        mBackMaxScale = 1.0f;
        mBmpWidth = scale * mOriBmpWidth;
        mBmpHeight = scale * mOriBmpHeight;
        calcRedundantSpace();

        float Xoffset = mCurrentScale * mBmpWidth - mFirstFillRect.width();
        float Yoffset = mCurrentScale * mBmpHeight - mFirstFillRect.height();

        dx = mFirstFillRect.left - Xoffset / 2.0f;
        dy = mFirstFillRect.top - Yoffset / 2.0f;

        mMatrix.setScale(scale, scale);
        //FIXME 后续修改成上下左右中四个方向
        if (mImageAtLeftOrRight) {
            //图片靠左边显示
            mMatrix.postTranslate(0, Math.round(dy));
        } else {
            //图片靠右边显示
            mMatrix.postTranslate(2 * dx, Math.round(dy));
        }
//        mMatrix.postTranslate(Math.round(dx), Math.round(dy));
        setImageMatrix(mMatrix);
        checkIsOnSide();
    }

    public void setScaleMode(int mode) {
        mScaleType = mode;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = getWidth();
        mHeight = getHeight();
        calcBitmapWH();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void setup() {
        if (mBitmap != null) {
            mOriBmpWidth = mBitmap.getWidth();
            mOriBmpHeight = mBitmap.getHeight();
            mBmpWidth = 0;
            mBmpHeight = 0;
            calcBitmapWH();
        }
    }

    public float getBmpWidth() {
        return mBmpWidth;
    }

    /**
     * 双击切换图片的显示样式，一种是窄边顶满屏幕，一种是长边顶满屏幕
     */
    public void onDubleClick(MotionEvent event) {
        if (mMinScale == mZommMaxScale) {
            return;
        }
        float x = event.getRawX() - getLeft();
        float y = event.getRawY() - getTop();
        mCenter.set(x, y);
        if (mCurrentScale != mZommMaxScale) {
            startScaleAnim(mCurrentScale, mZommMaxScale);
        } else {
            startScaleAnim(mCurrentScale, mMinScale);
        }
    }

    /**
     * 用于设置viewpager切换时黄框在图片左侧还是右侧
     * @param isLeft
     */
    public void setImageAtLeftOrRight(boolean isLeft) {
        //TODO 后续修改成上下左右中四个方向
        mImageAtLeftOrRight = isLeft;

    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        mBitmap = bm;
        setup();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        mBitmap = getBitmapFromDrawable(drawable);
        setup();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        super.setImageResource(resId);
        mBitmap = getBitmapFromDrawable(getDrawable());
        setup();
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        mBitmap = uri != null ? getBitmapFromDrawable(getDrawable()) : null;
        setup();
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        try {
            Bitmap bitmap;

            if (drawable instanceof ColorDrawable) {
                bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setParentCanScroll(boolean can) {
        mParentCanScroll = can;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!mParentCanScroll) { //如果父控件是不可滑动的，那么手势又自己传递
            //如果是可以滑动的，手势是又父控件的手势中直接调用
            hanleTouchEvent(event, true);
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    private void checkIntoDrag(boolean parentIsOnEdge) {
        if (parentIsOnEdge
                && ((mRedundantXSpace > 0 && mRedundantYSpace >= 0)
                || (mRedundantYSpace > 0 && mRedundantXSpace >= 0))
                ) {
            mMode = DRAG;
            mIsFirstMove = true;
        }
    }

    private boolean mIsFirstMove; //有静到拖动
    private boolean mIsFirstScale; //由静止到开始双指缩放
    private long mLastDragTime;

    public int hanleTouchEvent(MotionEvent event, boolean parentIsOnEdge) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mScrollDistance = 0;
                mMode = NONE;
                mPointCount = 1;
                checkIntoDrag(parentIsOnEdge);
                mLastX = event.getX(0);
                mLastY = event.getY(0);
                mVelocityTracker = VelocityTracker.obtain();
                if (mVelocityTracker != null){
                    //将当前的事件添加到检测器中
                    mVelocityTracker.addMovement(event);
                }
                //当手指再次点击到图片时，停止图片的惯性滑动
                if (mFlingRunnable != null){
                    mFlingRunnable.cancelFling();
                    mFlingRunnable = null;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mPointCount++;
                if (!mCanZoom || mPointCount > 2 || mMode == ZOOM_ANIM) {
                    break;
                }
                mOldDistant = getDistance(event);
                if (mOldDistant > mTouchSlop * 2) {
                    mMode = ZOOM;
                    mIsFirstScale = true;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mPointCount--;
                if (mPointCount > 1) {
                    break;
                }
                mMode = NONE;
                if (mCurrentScale < mBackMinScale) {
                    if (parentIsOnEdge) {
                        mMode = ZOOM_ANIM;
                    }
                    startScaleAnim(mCurrentScale, mBackMinScale);
                } else if (mCurrentScale > mBackMaxScale) {
                    if (parentIsOnEdge) {
                        mMode = ZOOM_ANIM;
                    }
                    startScaleAnim(mCurrentScale, mBackMaxScale);
                } else {
                    checkIntoDrag(parentIsOnEdge);
                    if (mMode == DRAG) {
                        int pointerIndex = event.getActionIndex();
                        mLastX = event.getX(1 - pointerIndex);
                        mLastY = event.getY(1 - pointerIndex);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mMode == DRAG) {
                    float currentX = event.getX(0);
                    float currentY = event.getY(0);
                    float deltaX = currentX - mLastX;
                    float deltaY = currentY - mLastY;
                    if (mIsFirstMove) { //第一次，由静到动，移动距离应该大于touchslop,防抖
                        if (Math.abs(deltaX) > mTouchSlop || Math.abs(deltaY) > mTouchSlop) {
                            mIsFirstMove = false;
                            mScrollDistance = 0;
                            mLastX = currentX;
                            mLastY = currentY;
                            break;
                        } else {
                            break;
                        }
                    }
                    mLastX = currentX;
                    mLastY = currentY;
                    //由拖动图片转向viewpager滑动的过程
                    if (mParentCanScroll) {
                        //在viewpager中，父容器可以滑动，所以要时刻判断图片是否拖动到了边框
//                        LogHelper.d("zoomview", "mRedundantXSpace mIsOnRightSide deltaX = " + mRedundantXSpace + ", " + mIsOnRightSide + ", " + deltaX);
                        if (mRedundantXSpace <= 0) { // 说明x方向图片和最小框已经重合了，不能响应图片左右拖动，只能响应上下拖动
                            if (Math.abs(deltaX) > Math.abs(deltaY)
//									&& Math.abs(deltaX) > mTouchSlop
                                    ) { //说明当前用户想要左右拖动，应该使父容易滚动
                                event.setAction(MotionEvent.ACTION_DOWN);
                                mMode = NONE; //返回none，父viewpager就会自己处理事件
                                break;
                            }
                        } else if ((Math.abs(deltaX) > Math.abs(deltaY))
//                                && Math.abs(deltaX) > mTouchSlop //首先判断是在左右滑动手势中
                                && ((mIsOnLeftSide && deltaX > 0) //如果滑到了左边缘，并且继续向右滑动，
                                || (mIsOnRightSide && deltaX < 0))) { // 如果滑到了右边缘，并且继续向左滑动 应该让父容器接受事件
                            event.setAction(MotionEvent.ACTION_DOWN);
                            mMode = NONE; //返回none，父viewpager就会自己处理事件
                            break;
                        }
                    }
                    if (mLastDragTime == 0) {
                        mLastDragTime = System.currentTimeMillis();
                    }
                    Log.e("swc", "deltaX:" + deltaX);
                    if (mVelocityTracker != null){
                        //将当前事件添加到检测器中
                        mVelocityTracker.addMovement(event);
                    }

                    checkAndSetTranslate(deltaX, deltaY);
                } else if (mMode == ZOOM) {
                    mCurrentDistant = getDistance(event);
                    float scaleFactor = mCurrentDistant / mOldDistant;
                    float deltaScale = Math.abs(scaleFactor - 1.0f);
                    if (deltaScale < 0.001) {
                        break;
                    }

                    if (mIsFirstScale) { //初次开始动，总有个突兀的跳变，所以消除掉第一次，防抖
                        mIsFirstScale = false;
                        mOldDistant = mCurrentDistant;
                        break;
                    }

                    mOldDistant = mCurrentDistant;
                    if (scaleFactor > 1.05f) {
                        scaleFactor = 1.05f;
                    } else if (scaleFactor < 0.95f) {
                        scaleFactor = 0.95f;
                    }
                    getCenter(mCenter, event);
                    zoomImg(scaleFactor);
                }
//                else if (mMode == NONE && parentIsOnEdge) {
//					if (mPointCount == 1) {
//						checkIntoDrag(parentIsOnEdge);
//						mLastX = event.getX(0);
//						mLastY = event.getY(0);
//					} else if (mPointCount > 1) {
//						mOldDistant = getDistance(event);
//						if (mOldDistant > mTouchSlop * 2) {
//							mMode = ZOOM;
//							mIsFirstScale = true;
//						}
//					}
//				}
                break;
            case MotionEvent.ACTION_UP:
                if(mMode==DRAG){
                    if (mVelocityTracker != null){
                        //将当前事件添加到检测器中
                        mVelocityTracker.addMovement(event);
                        //计算当前的速度
                        mVelocityTracker.computeCurrentVelocity(1000);
                        //得到当前x方向速度
                        final float vX = mVelocityTracker.getXVelocity();
                        //得到当前y方向的速度
                        final float vY = mVelocityTracker.getYVelocity();
                        mFlingRunnable = new FlingRunnable(getContext());
                        //调用fling方法，传入控件宽高和当前x和y轴方向的速度
                        //这里得到的vX和vY和scroller需要的velocityX和velocityY的负号正好相反
                        //所以传入一个负值
                        mFlingRunnable.fling(getWidth(),getHeight(),(int)-vX,(int)-vY);
                        //执行run方法
                        post(mFlingRunnable);
                    }
                }
                mMode = NONE;
                mPointCount = 0;
                if (mCurrentScale > mMinScale && !mIsOnLeftSide && !mIsOnRightSide) {
                    mMode = DRAG;
                }
                mLastDragTime = 0;
                break;
            case MotionEvent.ACTION_CANCEL:
                //释放速度检测器
                if (mVelocityTracker != null){
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }
        return mMode;
    }

    private boolean mIsScaleAniming = false;

    private void startScaleAnim(float start, float end) {
        if (mIsScaleAniming) {
            return;
        }
        mIsScaleAniming = true;
        ValueAnimator anim = ValueAnimator.ofFloat(start, end);
        anim.setDuration(200);
        anim.setInterpolator(sInterpolator);
        if (zoomBacklistener == null) {
            zoomBacklistener = new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float f = (Float) animation.getAnimatedValue();
                    float scaleFactor = f / mCurrentScale;
                    zoomImg(scaleFactor);
                }
            };
        }
        anim.addUpdateListener(zoomBacklistener);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
//                checkAndSetTranslate(0, 0);
//				checkIsOnSide();
                mIsScaleAniming = false;
                mMode = NONE;
            }
        });
        anim.start();
    }

    /**
     * 检查并且重新设置缩放系数，防止越过一些边缘极限值
     * 例如超过了最大，最小缩放值，支持剪裁的情况下缩放到剪裁框大小。
     */
    private float checkAndSetScaleFactor(float scaleFactor) {
        float origScale = mCurrentScale;
        mCurrentScale *= scaleFactor;
        if (mCurrentScale > mZommMaxScale) {
            mCurrentScale = mZommMaxScale;
            scaleFactor = mCurrentScale / origScale;
        } else if (mCurrentScale < mZoomMinScale) {
            mCurrentScale = mZoomMinScale;
            scaleFactor = mCurrentScale / origScale;
        }
        calcRedundantSpace();
        if (scaleFactor == 1.0f) {
            return 1.0f;
        }
        return scaleFactor;
    }

    private void zoomImg(float f) {
        float scaleFactor = checkAndSetScaleFactor(f);
        if (scaleFactor == 1.0f) {
            return;
        }

        float px, py; //缩放的中心点，如果图片宽小于边框宽了，中点就是边框的中心，否则就是双指的中心
        px = mRedundantXSpace <= 0 ? mMinLimitRect.centerX() : mCenter.x;
        py = mRedundantYSpace <= 0 ? mMinLimitRect.centerY() : mCenter.y;
//        mMatrix.postScale(scaleFactor, scaleFactor);
        mMatrix.postScale(scaleFactor, scaleFactor, px, py);

        getMatrixXY(mMatrix);

        //X方向有裕量的情况下，图片边缘不能进入最小框
        if (mRedundantXSpace >= 0) {
            if (mMatrixX > mMinLimitRect.left) {
                Log.e("swc", "this scale big x");
                mMatrix.postTranslate(mMinLimitRect.left - mMatrixX, 0);
            } else if (mMatrixX + mRedundantXSpace < mMinLimitRect.left) {
                Log.e("swc", "this scale small x");
                mMatrix.postTranslate(mMinLimitRect.left - mRedundantXSpace - mMatrixX, 0);
            }
        }
//        if (mRedundantXSpace < 0) {
//            mMatrix.postTranslate(0, (mMinLimitRect.height() - mBackMinScale * mBmpHeight) / 2);
//        }
        if (mRedundantYSpace >= 0) {
            if (mMatrixY > mMinLimitRect.top) {
                Log.e("swc", "this scale big y");
                mMatrix.postTranslate(0, mMinLimitRect.top - mMatrixY);
            } else if (mMatrixY + mRedundantYSpace < mMinLimitRect.top) {
                Log.e("swc", "this scale small y");
                mMatrix.postTranslate(0, mMinLimitRect.top - mRedundantYSpace - mMatrixY);
            }
        }
        setImageMatrix(mMatrix);
        checkIsOnSide();
        if (mOnImageScrollListener != null) {
            mOnImageScrollListener.onScroll(mMatrixX / mCurrentScale, mMatrixY / mCurrentScale);
        }
    }

    private float getDistance(MotionEvent event) {
        float x = event.getX(1) - event.getX(0);
        float y = event.getY(1) - event.getY(0);
        return (float) Math.sqrt((x * x + y * y));
    }

    private PointF getCenter(PointF centerF, MotionEvent event) {
        float x = (event.getX(1) + event.getX(0)) / 2;
        float y = (event.getY(1) + event.getY(0)) / 2;
        centerF.set(x, y);
        return centerF;
    }

    public boolean isAlreadyLoadBigBmp() {
        return mAlreadyLoadBigBmp;
    }

    public void setAlreadyLoadBigBmp(boolean alreadyLoadBigBmp) {
        mAlreadyLoadBigBmp = alreadyLoadBigBmp;
    }

    /**
     * 因为图片边缘不能进入最小框，所以需要知道最小框和图片宽高之间的差值，
     * 即x，y方向的冗余量，负数说明图片小于最小框
     */
    private void calcRedundantSpace() {
        mRedundantXSpace = mCurrentScale * mBmpWidth - mMinLimitRect.width();
        Log.e("swc", "mRedundantXSpace:" + mRedundantXSpace);
        mRedundantYSpace = mCurrentScale * mBmpHeight - mMinLimitRect.height();
    }

    private void checkAndSetTranslate(float deltaX, float deltaY) {
        getMatrixXY(mMatrix);
        if (mRedundantXSpace <= 0) {
            if (mMatrixX != mMinLimitRect.left) {
                deltaX = mMinLimitRect.left - mMatrixX;
            } else {
                deltaX = 0;
            }
        } else {
            if (mMatrixX + deltaX > mMinLimitRect.left) { //移动完后图片就进入最小框左边缘了，需要处理
                deltaX = mMinLimitRect.left - mMatrixX;
            } else if (mMatrixX + deltaX < mMinLimitRect.left - mRedundantXSpace) {
                deltaX = mMinLimitRect.left - mRedundantXSpace - mMatrixX;
            }
        }

        if (mRedundantYSpace <= 0) {
            if (mMatrixY != mMinLimitRect.top) {
                deltaY = mMinLimitRect.top - mMatrixY;
            } else {
                deltaY = 0;
            }
        } else {
            if (mMatrixY + deltaY > mMinLimitRect.top) {
                deltaY = mMinLimitRect.top - mMatrixY;
            } else if (mMatrixY + deltaY < mMinLimitRect.top - mRedundantYSpace) {
                deltaY = mMinLimitRect.top - mRedundantYSpace - mMatrixY;
            }
        }
        if (deltaX != 0 || deltaY != 0) {
            mScrollDistance = Math.max(mScrollDistance, Math.max(Math.abs(deltaX), Math.abs(deltaY)));
            Log.e("swc", "mScrollDistance:" + deltaX);
            Log.e("swc", "mMatrixX:" + mMatrixX);
            mMatrix.postTranslate(deltaX, deltaY);
//            scrollTo((int) deltaX,(int) deltaY);

            setImageMatrix(mMatrix);
            checkIsOnSide();
            if (mOnImageScrollListener != null) {
                mOnImageScrollListener.onScroll(mMatrixX / mCurrentScale, mMatrixY / mCurrentScale);
            }
        }
    }

    private void checkIsOnSide() {
        getMatrixXY(mMatrix);
        mIsOnLeftSide = false;
        mIsOnRightSide = false;
        mIsOnTopSide = false;
        mIsOnBottomSide = false;
        if (Math.abs(mMatrixX - mMinLimitRect.left) <= 0) {
            mIsOnLeftSide = true;
        }
        if (Math.abs(mMatrixX + mRedundantXSpace - mMinLimitRect.left) <= 0) {
            mIsOnRightSide = true;
        }
        if (Math.abs(mMatrixY - mMinLimitRect.top) <= 0) {
            mIsOnTopSide = true;
        }
        if (Math.abs(mMatrixY + mRedundantYSpace - mMinLimitRect.top) <= 0) {
            mIsOnBottomSide = true;
        }
    }

    private void getMatrixXY(Matrix m) {
        m.getValues(mTmpArray);
        mMatrixX = mTmpArray[Matrix.MTRANS_X];
        mMatrixY = mTmpArray[Matrix.MTRANS_Y];
    }

    public float getReDundantXSpace() {
        return mRedundantXSpace;
    }

    public void setCanZoom(boolean canZoom) {
        mCanZoom = canZoom;
    }

    public int getTouchMode() {
        return mMode;
    }

    public boolean canHorizontalDrag() {
        if (mCurrentScale >= mMinScale && mRedundantXSpace > 0) {
            return true;
        }
        return false;
    }

    public boolean canVerticalDrag() {
        if (mCurrentScale >= mMinScale && mRedundantYSpace > 0) {
            return true;
        }
        return false;
    }

    public float getCurrentScale() {
        return mCurrentScale;
    }

    public boolean isOnTopSide() {
        return mIsOnTopSide;
    }

    public boolean isOnBottomSide() {
        return mIsOnBottomSide;
    }

    public boolean isOnLeftSide() {
        return mIsOnLeftSide;
    }

    public boolean isOnRightSide() {
        return mIsOnRightSide;
    }

    public void setMinLimitRect(RectF rect) {
        mMinLimitRect = rect;
    }

    public RectF getMinLimitRect() {
        return mMinLimitRect;
    }

    public void setMaxMinScale(float max, float min) {
        mZommMaxScale = max;
        mZoomMinScale = min;
    }

    public Bitmap getOriginalBmp() {
        return mBitmap;
    }

    public Matrix getmMatrix() {
        return mMatrix;
    }
    /**
     * 获得缩放后图片的上下左右坐标以及宽高
     */
    private RectF getMatrixRectF(){
        //获得当钱图片的矩阵
        Matrix matrix = mMatrix;
        //创建一个浮点类型的矩形
        RectF rectF = new RectF();
        //得到当前的图片
        Drawable d = getDrawable();
        if (d != null){
            //使这个矩形的宽和高同当前图片一致
            rectF.set(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
            //将矩阵映射到矩形上面，之后我们可以通过获取到矩阵的上下左右坐标以及宽高
            //来得到缩放后图片的上下左右坐标和宽高
            matrix.mapRect(rectF);
        }
        return rectF;
    }
    public float getScrollDistance() {
        return mScrollDistance;
    }
    /**
     * 惯性滑动
     */
    private class FlingRunnable implements Runnable{
        private Scroller mScroller;
        private int mCurrentX , mCurrentY;

        public FlingRunnable(Context context){
            mScroller = new Scroller(context);
        }

        public void cancelFling(){
            mScroller.forceFinished(true);
        }

        /**
         * 这个方法主要是从onTouch中或得到当前滑动的水平和竖直方向的速度
         * 调用scroller.fling方法，这个方法内部能够自动计算惯性滑动
         * 的x和y的变化率，根据这个变化率我们就可以对图片进行平移了
         */
        public void fling(int viewWidth , int viewHeight , int velocityX ,
                          int velocityY){
            RectF rectF = getMatrixRectF();
            if (rectF == null){
                return;
            }
            final int startX = Math.round(-rectF.left);
            final int minX , maxX , minY , maxY;
            if (rectF.width() > viewWidth){
                minX = 0;
                maxX = Math.round(rectF.width() - viewWidth);
            }else{
                minX = maxX = startX;
            }
            final int startY = Math.round(-rectF.top);
            if (rectF.height() > viewHeight){
                minY = 0;
                maxY = Math.round(rectF.height() - viewHeight);
            }else{
                minY = maxY = startY;
            }
            mCurrentX = startX;
            mCurrentY = startY;

            if (startX != maxX || startY != maxY){
                mScroller.fling(startX,startY,velocityX,velocityY,minX,maxX,minY,maxY);
            }

        }

        /**
         * 每隔16ms调用这个方法，实现惯性滑动的动画效果
         */
        @Override
        public void run() {
            if (mScroller.isFinished()){
                return;
            }
            if (mScroller.computeScrollOffset()){
                final int newX = mScroller.getCurrX();
                final int newY = mScroller.getCurrY();
                mMatrix.postTranslate(mCurrentX-newX , mCurrentY-newY);
                checkIsOnSide();
                setImageMatrix(mMatrix);
                getMatrixXY(mMatrix);
                if (mOnImageScrollListener != null) {
                    mOnImageScrollListener.onScroll(mMatrixX / mCurrentScale, mMatrixY / mCurrentScale);
                }
                mCurrentX = newX;
                mCurrentY = newY;
                //每16ms调用一次
                postDelayed(this,16);
            }
        }
    }

    private OnImageScrollListener mOnImageScrollListener;

    public void setOnImageScrollListener(OnImageScrollListener onImageScrollListener) {
        mOnImageScrollListener = onImageScrollListener;
    }

    /**
     * 图片滑动监听
     */
    public interface OnImageScrollListener {
        void onScroll(float x, float y);
    }
}
