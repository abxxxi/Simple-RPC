package com.simplerpc.transport.netty.handler;

import com.simplerpc.common.RpcRequest;
import com.simplerpc.common.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RPC服务器处理器，用于处理客户端发送的请求
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private final Map<String, Object> serviceMap;
    private final ExecutorService executor;

    /**
     * 构造函数
     *
     * @param serviceMap 服务实例映射
     */
    public RpcServerHandler(Map<String, Object> serviceMap) {
        this.serviceMap = serviceMap;
        // 创建线程池，处理请求
        this.executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) {
        executor.submit(() -> {
            log.info("收到请求: {}", request.getRequestId());

            // 构建服务名
            String serviceName = request.getInterfaceName();
            if (request.getVersion() != null && !request.getVersion().isEmpty()) {
                serviceName += "-" + request.getVersion();
            }

            // 查找服务实例
            Object serviceBean = serviceMap.get(serviceName);

            if (serviceBean == null) {
                sendResponse(ctx, RpcResponse.fail(
                        request.getRequestId(), 404, "服务不存在: " + serviceName));
                return;
            }

            try {
                // 调用方法
                Object result = invokeMethod(serviceBean, request);

                // 返回响应
                sendResponse(ctx, RpcResponse.success(request.getRequestId(), result));
            } catch (Exception e) {
                log.error("处理请求失败", e);
                sendResponse(ctx, RpcResponse.fail(
                        request.getRequestId(), 500, "处理请求失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 调用服务方法
     *
     * @param serviceBean 服务实例
     * @param request RPC请求
     * @return 调用结果
     * @throws Exception 调用异常
     */
    private Object invokeMethod(Object serviceBean, RpcRequest request) throws Exception {
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        // 获取方法
        Method method = serviceClass.getMethod(methodName, parameterTypes);
        method.setAccessible(true);

        // 调用方法
        return method.invoke(serviceBean, parameters);
    }

    /**
     * 发送响应
     *
     * @param ctx 通道上下文
     * @param response RPC响应
     */
    private void sendResponse(ChannelHandlerContext ctx, RpcResponse response) {
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("RPC服务器处理异常", cause);
        ctx.close();
    }
}
