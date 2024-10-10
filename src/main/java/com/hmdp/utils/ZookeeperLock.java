package com.hmdp.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import java.util.concurrent.TimeUnit;

public class ZookeeperLock {
    private String path;
    private InterProcessMutex lock ;

    public ZookeeperLock(CuratorFramework client ,String name){
        this.path = "/locks/"+name;

        this.lock = new InterProcessMutex(client ,this.path);
    }
    public boolean tryLock () throws Exception {
        return lock.acquire(1200L, TimeUnit.SECONDS);
    }
    public void unLock() throws Exception {
        lock.release();
    }
}
