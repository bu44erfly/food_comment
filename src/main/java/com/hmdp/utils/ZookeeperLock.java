package com.hmdp.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import java.util.concurrent.TimeUnit;

public class ZookeeperLock {
    private CuratorFramework client;
    private String path;
    private InterProcessMutex lock ;

    public ZookeeperLock(CuratorFramework client, String path){
        this.client = client;
        this.path = path;
        this.lock = new InterProcessMutex(client, path);
    }
    public boolean tryLock () throws Exception {
        return lock.acquire(1200L, TimeUnit.SECONDS);
    }
    public void unLock() throws Exception {
        lock.release();
    }
}
