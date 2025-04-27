package com.simplerpc.transport;

import com.simplerpc.common.RpcRequest;
import com.simplerpc.common.RpcResponse;
import com.simplerpc.common.URL;

/**
 * RPC客户端接口，定义了发送请求的方法
 */
public interface  RpcClient {
    /**
     * 发送RPC请求
     *
     * @param url 服务地址
     * @param request RPC请求
     * @return RPC响应
     */
    RpcResponse send(URL url, RpcRequest request);

    /**
     * 关闭客户端
     */
    void close();
}
