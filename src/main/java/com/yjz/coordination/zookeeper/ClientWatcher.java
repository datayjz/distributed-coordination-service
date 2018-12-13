package com.yjz.coordination.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * Description:
 * Author: yjz
 * CreateDate: 2018-12-13 12:04 PM
 **/
public class ClientWatcher implements Watcher {

    public void process(WatchedEvent watchedEvent) {
        System.out.println(watchedEvent.getState());
    }
}
