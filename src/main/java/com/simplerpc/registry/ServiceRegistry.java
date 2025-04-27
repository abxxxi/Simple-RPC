package com.simplerpc.registry;

import com.simplerpc.common.URL;

import java.util.List;

/**
 * 服务注册接口，定义了服务注册和发现的方法
 */
public interface ServiceRegistry {
    /**
     * 注册服务
     *
     * @param serviceName 服务名称
     * @param url 服务地址
     */
    void register(String serviceName, URL url);

    /**
     * 注销服务
     *
     * @param serviceName 服务名称
     * @param url 服务地址
     */
    void unregister(String serviceName, URL url);

    /**
     * 发现服务
     *
     * @param serviceName 服务名称
     * @return 服务地址列表
     */
    List<URL> discover(String serviceName);

    /**
     * 关闭注册中心连接
     */
    void close();
}
