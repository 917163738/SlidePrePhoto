package cn.data.laoluo.rx_project;

import android.annotation.TargetApi;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.data.laoluo.rx_project.adapter.AdapterActivityDetailPageBase;
import cn.data.laoluo.rx_project.utils.DisplayUtil;
import cn.data.laoluo.rx_project.view.LinearLayoutManagerWithSmoothScroller;
import cn.data.laoluo.rx_project.view.PhotoLineView;
import cn.data.laoluo.rx_project.view.ViewPagerTransformer;
import cn.data.laoluo.rx_project.view.ZoomImageView;
import cn.data.laoluo.rx_project.view.ZoomImageViewPager;

public class PhotoActivity extends FragmentActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {
    private ZoomImageViewPager mVpMain;
    private PhotoLineView mLineView;
    private final List<Integer> mData = new ArrayList<Integer>(Arrays.asList(R.drawable.photo_a,
            R.drawable.photo_b, R.drawable.photo_c, R.drawable.photo_d, R.drawable.photo_e,
            R.drawable.photo_f, R.drawable.photo_g));
    private AdapterActivityDetailPageBase mPageAdapter;
    MyRecyclerView mRecyclerView;
    private GalleryAdapter mAdapter;
    private boolean mIsLineAtLeft=true;
    private int mOriginHeight;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 101) {
                if (mPageAdapter != null) {
                    onPageSelected(msg.arg1);
                }
            } else if (msg.what == 111) {

                View view = mRecyclerView.findViewHolderForLayoutPosition(mPageNum).itemView;
                mLineView.setLineWidth(view.getWidth());
                float x = mIsLineAtLeft?view.getLeft():view.getRight()-mLineView.getLineWidth();
                if (x < 0) {
                    x = 0;
                }
                flingX = x;
                mViewXoffset=x;
                Log.e("swc", "x:" + x);
                mLineView.setTranslationX(x);
                mLineView.setVisibility(View.VISIBLE);
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo);
        mVpMain = (ZoomImageViewPager) findViewById(R.id.vp_main);
        mRecyclerView = (MyRecyclerView) findViewById(R.id.id_recyclerview_horizontal);
        mLineView = (PhotoLineView) findViewById(R.id.photoLine);

        Point point = DisplayUtil.getRealScreenPoint(this);
        mLineView.setOriginSize(point.x, point.y);
        mVpMain.setOffscreenPageLimit(1); //设置vp的缓存数量
        mVpMain.setOnClickListener(this);
        mVpMain.setPageTransformer(true, new ViewPagerTransformer.ParallaxTransformer(R.id.iv_main_big_image));
        mPageAdapter = new AdapterActivityDetailPageBase(this);
        mPageAdapter.addMoreItem(mData);
        mVpMain.setAdapter(mPageAdapter);
        mVpMain.addOnPageChangeListener(this);
        mHandler.sendEmptyMessageDelayed(101, 300);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setLayoutManager(new LinearLayoutManagerWithSmoothScroller(this));
        mAdapter = new GalleryAdapter(this, mData);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(mOnScrollListener);
    }

    private float flingX,mViewXoffset = 0f;
    private OnScrollListener mOnScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                Log.e("swc","onScrolled:"+dx);
            flingX -= dx;
            mViewXoffset-=dx;
            mLineView.setTranslationX(flingX);
            super.onScrolled(recyclerView, dx, dy);
        }
    };

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    private int mPageNum = 0;

    @Override
    public void onPageSelected(int position) {
//        mHandler.obtainMessage(101, position).sendToTarget();
        Log.e("swc", "postion:" + position);
        if(mPageNum<=position){
            mIsLineAtLeft=true;
        }else {
            mIsLineAtLeft=false;
        }
        mPageNum = position;
        mPageAdapter.onPageSelected(position,mIsLineAtLeft);
//        mRecyclerView.smoothScrollToPosition(position);
        AdapterActivityDetailPageBase.ViewHolder currentHolder = mPageAdapter.getCurrentHolder();
        if (currentHolder != null) {
            mVpMain.setZoomImageView(currentHolder.image);
        }
        mRecyclerView.getLayoutManager().scrollToPosition(position);
        mLineView.setVisibility(View.INVISIBLE);
        mHandler.removeMessages(111);
        mHandler.sendEmptyMessageDelayed(111, 300);
        currentHolder.image.setOnImageScrollListener(mImageScrollListener);
//        float x=mRecyclerView.findViewHolderForLayoutPosition(position).itemView.getLeft();
//        if(x<0){
//            x=0;
//        }
//        Log.e("swc", "x:" + x);
//                mLineView.setTranslationX(x);

    }

    private ZoomImageView.OnImageScrollListener mImageScrollListener = new ZoomImageView.OnImageScrollListener() {
        @Override
        public void onScroll(float x, float y) {
            //FIXME 出现第二张图片获取的宽度是第一张图片的问题，暂未发现原因
            float bottomW=mAdapter.getViewHolder().get(mPageNum).mImg.getWidth();
            float dis =  (x *  bottomW/ mPageAdapter.getCurrentHolder().image.getBmpWidth());
            float leftX=bottomW-mLineView.getLineWidth();
            if(mIsLineAtLeft){
                flingX =mViewXoffset- dis;
                if(flingX<mViewXoffset||flingX>mViewXoffset+leftX){
                    return;
                }
            }else {
                dis=leftX+dis;
                flingX=mViewXoffset-dis;
                float minX=mViewXoffset+mLineView.getLineWidth()-bottomW;
                float maxX=mViewXoffset;
                if(flingX>maxX||flingX<minX){
                    return;
                }


            }
            mLineView.setTranslationX(flingX);
        }
    };

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onClick(View view) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecyclerView.removeOnScrollListener(mOnScrollListener);
    }
}
