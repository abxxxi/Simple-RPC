package com.simplerpc.transport;

/**
 * RPC服务器接口，定义了启动和停止服务器的方法
 */
public interface RpcServer {
    /**
     * 启动服务器
     *
     * @param port 监听端口
     */
    void start(int port);

    /**
     * 停止服务器
     */
    void stop();
}
