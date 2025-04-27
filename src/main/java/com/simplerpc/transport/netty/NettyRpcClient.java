package com.simplerpc.transport.netty;

import com.simplerpc.common.RpcRequest;
import com.simplerpc.common.RpcResponse;
import com.simplerpc.common.URL;
import com.simplerpc.serialize.Serializer;
import com.simplerpc.serialize.impl.HessianSerializer;
import com.simplerpc.transport.RpcClient;
import com.simplerpc.transport.netty.codec.RpcDecoder;
import com.simplerpc.transport.netty.codec.RpcEncoder;
import com.simplerpc.transport.netty.handler.RpcClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 基于Netty的RPC客户端实现
 */
@Slf4j
public class NettyRpcClient implements RpcClient {

    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final Serializer serializer;

    // 连接池，避免频繁创建连接
    private final Map<String, Channel> channelPool = new ConcurrentHashMap<>();
    // 存储请求响应的映射
    private final Map<String, CompletableFuture<RpcResponse>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 构造函数
     */
    public NettyRpcClient() {
        // 默认使用Hessian序列化器
        this(new HessianSerializer());
    }

    /**
     * 构造函数
     *
     * @param serializer 序列化器
     */
    public NettyRpcClient(Serializer serializer) {
        this.serializer = serializer;
        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();

        // 配置Netty客户端
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 添加编解码器
                        pipeline.addLast(new RpcEncoder(serializer, RpcRequest.class));
                        pipeline.addLast(new RpcDecoder(serializer, RpcResponse.class));
                        // 添加请求处理器
                        pipeline.addLast(new RpcClientHandler(pendingRequests));
                    }
                });
    }

    @Override
    public RpcResponse send(URL url, RpcRequest request) {
        try {
            // 获取连接
            Channel channel = getChannel(url);

            // 创建响应Future
            CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
            pendingRequests.put(request.getRequestId(), responseFuture);

            // 发送请求
            channel.writeAndFlush(request).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    responseFuture.completeExceptionally(future.cause());
                    pendingRequests.remove(request.getRequestId());
                }
            });

            // 等待响应，最多等待5秒
            return responseFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("发送RPC请求失败", e);
            return RpcResponse.fail(request.getRequestId(), 500, "发送RPC请求失败: " + e.getMessage());
        } finally {
            // 清理请求映射
            pendingRequests.remove(request.getRequestId());
        }
    }

    /**
     * 获取或创建连接
     *
     * @param url 服务地址
     * @return 连接通道
     */
    private Channel getChannel(URL url) throws InterruptedException {
        String address = url.getAddress();

        // 尝试从连接池获取
        Channel channel = channelPool.get(address);

        // 检查连接是否可用
        if (channel == null || !channel.isActive()) {
            // 创建新连接
            ChannelFuture future = bootstrap.connect(url.getHost(), url.getPort()).sync();
            if (future.isSuccess()) {
                channel = future.channel();
                channelPool.put(address, channel);

                // 添加连接关闭的监听器，从连接池中移除
                channel.closeFuture().addListener((ChannelFutureListener) closeFuture ->
                        channelPool.remove(address));
            } else {
                throw new RuntimeException("连接服务器失败: " + url.getAddress());
            }
        }

        return channel;
    }

    @Override
    public void close() {
        // 关闭所有连接
        for (Channel channel : channelPool.values()) {
            channel.close();
        }
        channelPool.clear();
        pendingRequests.clear();

        // 关闭线程组
        group.shutdownGracefully();
        log.info("Netty客户端已关闭");
    }
}
