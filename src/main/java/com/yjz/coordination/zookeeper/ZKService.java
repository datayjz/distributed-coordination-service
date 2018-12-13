package com.yjz.coordination.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Description:
 * Author: yjz
 * CreateDate: 2018-12-13 11:37 AM
 **/
public class ZKService {

    private Logger LOG = LoggerFactory.getLogger(ZKService.class);

    private ZooKeeper zooKeeper;
    private ClientWatcher clientWatcher;

    public ZKService() {
        try {
            clientWatcher = new ClientWatcher();
            zooKeeper = new ZooKeeper("192.168.0.1:2181", 5000, clientWatcher);
        }catch (IOException e) {
            e.printStackTrace();
            LOG.error("Connect zookeeper fail! because:{}",e.getMessage());
        }
    }

    public boolean syncCreateZnode(String path, byte[] data, List<ACL> acls,CreateMode createMode) {
        boolean result = true;
        try {
            if(!syncZnodeExist(path)) {
                zooKeeper.create(path,data,acls, createMode);
            } else {
                result = false;
                LOG.warn("Path [{}] is exit,can not create!");
            }
        }catch (Exception e) {
            result = false;
            e.printStackTrace();
            LOG.error("Create {} Znode fail!",path);
        }
        return result;
    }

    public boolean syncZnodeExist(String path) {
        boolean result = false;
        try {
            if(zooKeeper.exists(path,false) != null) {
                result = true;
            }
        }catch (Exception e) {
            e.printStackTrace();
            LOG.error("");
        }
        return result;
    }
}
