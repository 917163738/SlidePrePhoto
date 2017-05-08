package cn.data.laoluo.rx_project.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.data.laoluo.rx_project.R;
import cn.data.laoluo.rx_project.utils.DisplayUtil;
import cn.data.laoluo.rx_project.view.ZoomImageView;


public class AdapterActivityDetailPageBase extends PagerAdapter implements View.OnClickListener {
    public final String TAG = "AdapterActivityDetail";
    protected final Activity mContext; //用activity，利用glide的生命周期控制系统
    private ArrayList<ViewHolder> mHolders = new ArrayList<>();
    private int mScreenW, mScreenH;
    private RectF mImageViewRect;
    private int mCurrentPosition;
    private ViewHolder mCurrentViewHolder;
    private List<Integer> mData;

    public AdapterActivityDetailPageBase(Activity context) {
        mContext = context;
        sparseArray = new SparseArray<>();
        Point point = DisplayUtil.getRealScreenPoint(mContext);
        mScreenW = point.x;
        mScreenH = point.y;
        mImageViewRect = new RectF(0, 0, mScreenW, mScreenH);
    }


    public void addMoreItem(List<Integer> list) {
        if (list != null && list.size() > 0) {
            mData=list;
            notifyDataSetChanged();
        }
    }

    //====================================================================
    private View fullScreenImgView;
    private SparseArray<View> sparseArray;
    private boolean mIsZutuDetail;


    /**
     * 首页用
     *
     * @param location
     * @param fullScreenImgView
     */
    public void setAdImageViewArray(int location, View fullScreenImgView) {
        sparseArray.put(location, fullScreenImgView);
        this.mIsZutuDetail = false;
    }
    //======================================================


    @Override
    public int getItemPosition(Object object) {
        return super.getItemPosition(object);
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return object == view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (object != null) {
            View view = (View) object;
            container.removeView(view);
            if (mData == null || mData.size() == 0) {
                return;
            }
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.image.setImageBitmap(null);
            holder.mCurrentBitmap = null;
            holder.imgState = 0;
            holder.position = -1;
//            Glide.clear(holder.image);
            mHolders.remove(holder);
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
//        final Integer imageBean = mData.get(position);


        View view = View.inflate(mContext, R.layout.activity_detailpage_item, null);
        ViewHolder holder = new ViewHolder(view);
        holder.position = position;
        view.setTag(holder);
        mHolders.add(holder);
        container.addView(holder.root);

        return holder.root;
    }


    public Bitmap getCurrentBitmap() {
        if (mCurrentViewHolder != null) {
            return mCurrentViewHolder.mCurrentBitmap;
        }
        return null;
    }

    public ViewHolder getCurrentHolder() {
        return mCurrentViewHolder;
    }

    public void onPageSelected(final int position) {
        mCurrentPosition = position;
        for (int i = 0; i < mHolders.size(); i++) {
            ViewHolder temp = mHolders.get(i);
            if (temp.position == mCurrentPosition) {
                mCurrentViewHolder = temp;
                break;
            }
        }
        loadBigImg(mCurrentViewHolder);
    }

    private void loadBigImg(final ViewHolder tempHolder) {
        if ( tempHolder == null) {
            return;
        }
        if ( tempHolder.position != -1) {
            if (tempHolder.position >= mData.size()) {
                return;
            }
            Integer bean = mData.get(tempHolder.position);
            tempHolder.image.setImageResource(bean);
            tempHolder.mCurrentBitmap=((BitmapDrawable)tempHolder.image.getDrawable()).getBitmap();
        }
        tempHolder.loadingView.setVisibility(View.GONE);
        tempHolder.errorView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fail_layout:
                break;
            default:
                break;
        }
    }

    public class ViewHolder {
        public final View root;
        public Bitmap mCurrentBitmap;
        public final ZoomImageView image;
        public final View errorView;
        public final View loadingView;
        public int position = -1; //当前的图片加载的是第几个图，如果是-1，代表已经销毁
        /**
         * 加载图片的状态，0代表加载失败或者还没开始加载，什么图都没有的状态，1代表加载小图成功，2代表加载大图成功
         */
        public int imgState;

        ViewHolder(View view) {
            root = view;
            image = (ZoomImageView) root.findViewById(R.id.iv_main_big_image);
            image.setFirstFillRect(mImageViewRect);
            image.setMinLimitRect(mImageViewRect);
            image.setScaleType(ImageView.ScaleType.MATRIX);
            image.setScaleMode(1); //宽边符合宽高的
            image.setParentCanScroll(true);
            errorView = root.findViewById(R.id.fail_layout);
            loadingView = root.findViewById(R.id.layout_loading);
            loadingView.setOnClickListener(AdapterActivityDetailPageBase.this);
            errorView.setOnClickListener(AdapterActivityDetailPageBase.this);
        }
    }

    private boolean mIsDestory = false;

    public void destory() {
        mIsDestory = true;
        mHolders.clear();
    }

}
