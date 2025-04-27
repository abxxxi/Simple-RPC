package com.simplerpc.transport.netty;

import com.simplerpc.common.RpcRequest;
import com.simplerpc.common.RpcResponse;
import com.simplerpc.serialize.Serializer;
import com.simplerpc.serialize.impl.HessianSerializer;
import com.simplerpc.transport.RpcServer;
import com.simplerpc.transport.netty.codec.RpcDecoder;
import com.simplerpc.transport.netty.codec.RpcEncoder;
import com.simplerpc.transport.netty.handler.RpcServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Netty的RPC服务器实现
 */
@Slf4j
public class NettyRpcServer implements RpcServer {
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final Serializer serializer;

    // 服务实例映射，key为服务名(接口名+版本)，value为服务实例
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    private ChannelFuture channelFuture;

    /**
     * 构造函数
     */
    public NettyRpcServer() {
        // 默认使用Hessian序列化器
        this(new HessianSerializer());
    }

    /**
     * 构造函数
     *
     * @param serializer 序列化器
     */
    public NettyRpcServer(Serializer serializer) {
        this.serializer = serializer;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
    }

    /**
     * 添加服务实例
     *
     * @param serviceName     服务名称
     * @param serviceInstance 服务实例
     */
    public void addService(String serviceName, Object serviceInstance) {
        serviceMap.put(serviceName, serviceInstance);
        log.info("添加服务: {}", serviceName);
    }

    @Override
    public void start(int port) {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    // 添加编解码器
                                    .addLast(new RpcDecoder(serializer, RpcRequest.class))
                                    .addLast(new RpcEncoder(serializer, RpcResponse.class))
                                    // 添加请求处理器
                                    .addLast(new RpcServerHandler(serviceMap));
                        }
                    });

            // 绑定端口并启动服务器
            channelFuture = bootstrap.bind(port).sync();
            log.info("Netty RPC服务器已启动，监听端口: {}", port);

        } catch (Exception e) {
            log.error("Netty RPC服务器启动失败", e);
            throw new RuntimeException("Netty RPC服务器启动失败", e);
        }
    }

    @Override
    public void stop() {
        try {
            if (channelFuture != null) {
                // 关闭服务器通道
                channelFuture.channel().close().sync();
            }
        } catch (InterruptedException e) {
            log.error("关闭RPC服务器失败", e);
        } finally {
            // 关闭线程组
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            log.info("Netty RPC服务器已关闭");
        }
    }
}
