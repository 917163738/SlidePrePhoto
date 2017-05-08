package cn.data.laoluo.rx_project.java_rx;

/**
 * Created by luoliwen on 16/5/31.
 */
public class Test {

    public static void main(String[] args) throws Exception{
        Watched xiaoming = new ConcreteWatched();

        Watcher watcher1 = new ConcreteWatcher();
        Watcher watcher2 = new ConcreteWatcher();
        Watcher watcher3 = new ConcreteWatcher();

        xiaoming.addWatcher(watcher1);
        xiaoming.addWatcher(watcher2);
        xiaoming.addWatcher(watcher3);

        xiaoming.notifyWatchers("我要偷东西了");

    }
}
