package com.simplerpc.transport.netty.handler;

import com.simplerpc.common.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * RPC客户端处理器，用于处理服务器返回的响应
 */
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private final Map<String, CompletableFuture<RpcResponse>> pendingRequests;

    /**
     * 构造函数
     *
     * @param pendingRequests 挂起的请求映射
     */
    public RpcClientHandler(Map<String, CompletableFuture<RpcResponse>> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<RpcResponse> future = pendingRequests.get(requestId);

        if (future != null) {
            // 设置响应结果，唤醒等待线程
            future.complete(response);
            pendingRequests.remove(requestId);
        } else {
            log.warn("收到未知请求ID的响应: {}", requestId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("RPC客户端处理异常", cause);
        // 关闭连接
        ctx.close();
    }
}
