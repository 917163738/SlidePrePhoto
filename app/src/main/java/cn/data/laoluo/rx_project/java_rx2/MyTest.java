package cn.data.laoluo.rx_project.java_rx2;


import java.util.ArrayList;

/**
 * Created by luoliwen on 16/5/31.
 */
public class MyTest {

    public static void main(String[] args) throws Exception {
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }
        for (int i = 0; i < list.size(); i++) {
            System.out.println("i=" + list.get(i));
        }
//        SimpleObservable simple = new SimpleObservable();
//
//        SimpleObserver observer = new SimpleObserver(simple);
//
//        simple.setData(1);
//        simple.setData(2);
//        simple.setData(2);
//        simple.setData(3);

    }
}
