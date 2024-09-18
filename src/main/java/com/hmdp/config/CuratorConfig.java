package com.hmdp.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class CuratorConfig {

    private static final String ZK_ADDRESS = "127.0.0.1:2181";  // 替换为实际的ZooKeeper地址

    public static CuratorFramework createCuratorClient() {
        // 重试策略：初始等待时间1秒，最多重试3次
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);

        // 创建客户端并启动
        CuratorFramework client = CuratorFrameworkFactory.newClient(ZK_ADDRESS, retryPolicy);
        client.start();

        return client;
    }
}
