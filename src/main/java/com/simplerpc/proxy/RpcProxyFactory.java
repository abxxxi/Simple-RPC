package com.simplerpc.proxy;

import com.simplerpc.common.RpcRequest;
import com.simplerpc.common.RpcResponse;
import com.simplerpc.common.URL;
import com.simplerpc.registry.ServiceRegistry;
import com.simplerpc.transport.RpcClient;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * RPC代理工厂，用于创建远程服务的代理
 */
@Slf4j
public class RpcProxyFactory {

    private final RpcClient rpcClient;
    private final ServiceRegistry serviceRegistry;

    /**
     * 构造函数
     *
     * @param rpcClient       RPC客户端
     * @param serviceRegistry 服务注册中心
     */
    public RpcProxyFactory(RpcClient rpcClient, ServiceRegistry serviceRegistry) {
        this.rpcClient = rpcClient;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * 创建代理
     *
     * @param interfaceClass 接口类
     * @param version        版本号
     * @param timeout        超时时间
     * @param <T>            接口类型
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> interfaceClass, String version, long timeout) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        // 构建服务名
                        String serviceName = interfaceClass.getName();
                        if (version != null && !version.isEmpty()) {
                            serviceName += "-" + version;
                        }

                        // 构建请求
                        RpcRequest request = new RpcRequest();
                        request.setRequestId(UUID.randomUUID().toString());
                        request.setInterfaceName(interfaceClass.getName());
                        request.setMethodName(method.getName());
                        request.setParameterTypes(method.getParameterTypes());
                        request.setParameters(args);
                        request.setVersion(version);

                        // 从注册中心发现服务
                        List<URL> urls = serviceRegistry.discover(serviceName);

                        if (urls == null || urls.isEmpty()) {
                            throw new RuntimeException("无法找到服务: " + serviceName);
                        }

                        // 随机选择一个服务实例（简单的负载均衡）
                        URL url = urls.get(new Random().nextInt(urls.size()));

                        // 发送请求
                        RpcResponse response = rpcClient.send(url, request);

                        // 处理响应
                        if (response.getStatusCode() != 200) {
                            throw new RuntimeException(response.getMessage());
                        }

                        return response.getData();
                    }
                });
    }

}
