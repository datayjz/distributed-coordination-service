package com.yjz.coordination.zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;


public class ZKServiceTest {

    private ZKService zkService;

    @Before
    public void setUp() throws Exception {
        zkService = new ZKService();
    }

    @Test
    public void synCreateZnode() {
        ACL aclIp = new ACL(ZooDefs.Perms.READ,new Id("ip","192.165.0.1"));
        boolean result = zkService.syncCreateZnode("/test","Hello".getBytes(), Arrays.asList(aclIp), CreateMode.PERSISTENT);
        Assert.assertEquals(result,true);
    }

    @Test
    public void syncZnodeExist() {
        boolean result = zkService.syncZnodeExist("/test");
        Assert.assertEquals(result,true);
    }
}
