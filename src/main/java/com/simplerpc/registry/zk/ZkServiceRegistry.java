package com.simplerpc.registry.zk;

import com.simplerpc.common.URL;
import com.simplerpc.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于ZooKeeper的服务注册实现
 */
@Slf4j
public class ZkServiceRegistry implements ServiceRegistry {

    private static final String ZK_ROOT_PATH = "/simple-rpc";
    private final CuratorFramework zkClient;

    // 本地缓存，用于避免频繁查询ZooKeeper
    private final Map<String, List<URL>> serviceCache = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param zkAddress ZooKeeper地址，格式为host:port
     */
    public ZkServiceRegistry(String zkAddress) {
        // 创建ZooKeeper客户端
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .sessionTimeoutMs(60000)
                .connectionTimeoutMs(15000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .namespace("simple-rpc")
                .build();

        // 启动客户端
        zkClient.start();
        try {
            // 等待连接建立
            if (!zkClient.blockUntilConnected(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("连接ZooKeeper超时");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("连接ZooKeeper被中断", e);
        }

        log.info("ZooKeeper客户端已连接到{}", zkAddress);
    }

    @Override
    public void register(String serviceName, URL url) {
        // 服务路径，例如/simple-rpc/com.example.UserService/192.168.1.1:8080
        String servicePath = ZK_ROOT_PATH + "/" + serviceName;
        String instancePath = servicePath + "/" + url.getAddress();

        try {
            // 创建服务节点（持久节点）
            if (zkClient.checkExists().forPath(servicePath) == null) {
                zkClient.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(servicePath);
            }

            // 创建实例节点（临时节点，会话结束自动删除）
            zkClient.create()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(instancePath);

            log.info("服务{}注册成功，地址：{}", serviceName, url.getAddress());

            // 添加到本地缓存
            serviceCache.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(url);

        } catch (Exception e) {
            log.error("注册服务{}失败", serviceName, e);
            throw new RuntimeException("注册服务失败", e);
        }
    }

    @Override
    public void unregister(String serviceName, URL url) {
        // 服务实例路径
        String instancePath = ZK_ROOT_PATH + "/" + serviceName + "/" + url.getAddress();

        try {
            if (zkClient.checkExists().forPath(instancePath) != null) {
                zkClient.delete().forPath(instancePath);
            }

            // 从本地缓存中移除
            List<URL> urls = serviceCache.get(serviceName);
            if (urls != null) {
                urls.remove(url);
            }

            log.info("服务{}注销成功，地址：{}", serviceName, url.getAddress());

        } catch (Exception e) {
            log.error("注销服务{}失败", serviceName, e);
            throw new RuntimeException("注销服务失败", e);
        }
    }

    @Override
    public List<URL> discover(String serviceName) {
        // 从本地缓存中获取
        List<URL> cachedURLs = serviceCache.get(serviceName);
        if (cachedURLs != null && !cachedURLs.isEmpty()) {
            return new ArrayList<>(cachedURLs);
        }

        // 服务路径
        String servicePath = ZK_ROOT_PATH + "/" + serviceName;

        try {
            // 查询服务是否存在
            if (zkClient.checkExists().forPath(servicePath) != null) {
                return new ArrayList<>();
            }

            // 获取所有实例
            List<String> instances = zkClient.getChildren().forPath(servicePath);
            List<URL> urls = new ArrayList<>();

            for (String instance : instances) {
                URL url = URL.parse(instance);
                urls.add(url);
            }

            // 更新本地缓存
            serviceCache.put(serviceName, new ArrayList<>(urls));

            // 添加监听器，监听服务变化
            addServiceChangeListener(serviceName);

            log.info("服务{}查询成功，地址：{}", serviceName, urls);

            return urls;
        } catch (Exception e) {
            log.error("查询服务{}失败", serviceName, e);
            throw new RuntimeException("查询服务失败", e);
        }
    }

    /**
     * 添加服务监听器，监听服务实例变化
     *
     * @param serviceName 服务名称
     */
    private void addServiceChangeListener(String serviceName) {
        String servicePath = ZK_ROOT_PATH + "/" + serviceName;

        try {
            PathChildrenCache childrenCache = new PathChildrenCache(zkClient, servicePath, true);
            childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);

            // 添加监听器
            childrenCache.getListenable().addListener((client, event) -> {
                List<URL> urls = new ArrayList<>();

                if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED ||
                        event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED ||
                        event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED) {

                    // 服务发生变化，重新获取所有实例
                    List<String> instances = zkClient.getChildren().forPath(servicePath);

                    for (String instance : instances) {
                        urls.add(URL.parse(instance));
                    }

                    // 更新本地缓存
                    serviceCache.put(serviceName, urls);

                    log.info("服务{}的实例发生变化，更新为: {}", serviceName, urls);
                }
            });
        } catch (Exception e) {
            log.error("添加服务{}监听器失败", serviceName, e);
        }
    }

    @Override
    public void close() {
        if (zkClient != null) {
            zkClient.close();
            log.info("ZooKeeper客户端已关闭");
        }
    }
}
