package com.simplerpc.server;

import com.simplerpc.common.URL;
import com.simplerpc.common.annotation.RpcService;
import com.simplerpc.registry.ServiceRegistry;
import com.simplerpc.transport.RpcServer;
import com.simplerpc.transport.netty.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.net.InetAddress;
import java.util.Map;

/**
 * RPC服务提供者，用于启动RPC服务器并注册服务
 */
@Slf4j
public class RpcServiceProvider implements ApplicationContextAware, InitializingBean, DisposableBean{

    private final RpcServer rpcServer;
    private final ServiceRegistry serviceRegistry;
    private final String host;
    private final int port;

    private ApplicationContext applicationContext;

    /**
     * 构造函数
     *
     * @param serviceRegistry 服务注册中心
     * @param port 服务端口
     */
    public RpcServiceProvider(ServiceRegistry serviceRegistry, int port) throws Exception {
        this.serviceRegistry = serviceRegistry;
        this.port = port;
        this.host = InetAddress.getLocalHost().getHostAddress();
        this.rpcServer = new NettyRpcServer();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 扫描并注册所有标注了@RpcService的服务
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);

        if (serviceBeanMap != null && !serviceBeanMap.isEmpty()) {
            for (Object serviceBean : serviceBeanMap.values()) {
                RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);

                // 获取服务接口
                Class<?> interfaceClass = rpcService.interfaceClass();
                if (interfaceClass == void.class) {
                    // 如果未指定接口，则获取第一个实现的接口
                    interfaceClass = serviceBean.getClass().getInterfaces()[0];
                }

                // 构建服务名
                String serviceName = interfaceClass.getName();
                String version = rpcService.version();
                if (version != null && !version.isEmpty()) {
                    serviceName += "-" + version;
                }

                // 添加服务到RPC服务器
                ((NettyRpcServer) rpcServer).addService(serviceName, serviceBean);

                // 注册服务到注册中心
                URL url = new URL(host, port);
                serviceRegistry.register(serviceName, url);

                log.info("注册服务: {} => {}", serviceName, url.getAddress());
            }
        }

        // 启动RPC服务器
        rpcServer.start(port);
    }

    @Override
    public void destroy() {
        // 停止RPC服务器
        rpcServer.stop();
    }
}
