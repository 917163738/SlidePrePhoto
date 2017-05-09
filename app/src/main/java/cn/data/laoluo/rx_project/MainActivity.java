package cn.data.laoluo.rx_project;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import cn.data.laoluo.rx_project.android_rx.RxUtils;
import cn.data.laoluo.rx_project.drag.DragActivity;

public class MainActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }

    /**
     * 调用create 方法
     * @param view
     */
    public void createMethod(View view) {
        RxUtils.createObserable();
    }

    public  void  createMethod2(View view){
        RxUtils.createPrint();
    }

    public  void  from(View view){
        RxUtils.from();
    }

    public  void  just(View view){
        RxUtils.just();
    }

    public  void  filter(View view){
        RxUtils.filter();
    }
    public  void  viewpager(View view){
        Intent intent=new Intent(MainActivity.this,PhotoActivity.class);
        MainActivity.this.startActivity(intent);

    }
    public  void  drag(View view){
        Intent intent=new Intent(MainActivity.this,DragActivity.class);
        MainActivity.this.startActivity(intent);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
