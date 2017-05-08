package cn.data.laoluo.rx_project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;


public class GalleryActivity extends Activity
{

	private MyRecyclerView mRecyclerView;
	private GalleryAdapter mAdapter;
	private List<Integer> mDatas;
	private ImageView mImg ; 
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_gallery);
		
		mImg = (ImageView) findViewById(R.id.id_content);

		mDatas = new ArrayList<Integer>(Arrays.asList(R.drawable.a,
				R.drawable.b, R.drawable.c, R.drawable.d, R.drawable.e,
				R.drawable.f, R.drawable.g, R.drawable.h, R.drawable.l));

		mRecyclerView = (MyRecyclerView) findViewById(R.id.id_recyclerview_horizontal);
		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
		linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

		mRecyclerView.setLayoutManager(linearLayoutManager);
		mAdapter = new GalleryAdapter(this, mDatas);
		mRecyclerView.setAdapter(mAdapter);

		mRecyclerView.setOnItemScrollChangeListener(new MyRecyclerView.OnItemScrollChangeListener()
		{
			@Override
			public void onChange(View view, int position)
			{
				mImg.setImageResource(mDatas.get(position));
			};
		});
		
		mAdapter.setOnItemClickLitener(new GalleryAdapter.OnItemClickLitener()
		{
			@Override
			public void onItemClick(View view, int position)
			{
//				Toast.makeText(getApplicationContext(), position + "", Toast.LENGTH_SHORT)
//						.show();
				mImg.setImageResource(mDatas.get(position));
			}
		});

	}

}
