package com.simplerpc.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RPC响应对象，包含调用结果或异常信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse {
    /**
     * 对应请求的ID
     */
    private String requestId;

    /**
     * 响应状态码
     */
    private Integer statusCode;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 返回数据
     */
    private Object data;

    /**
     * 成功响应
     */
    public static RpcResponse success(String requestId, Object data) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setStatusCode(200);
        response.setMessage("调用成功");
        response.setData(data);
        return response;
    }

    /**
     * 失败响应
     */
    public static RpcResponse fail(String requestId, Integer code, String message) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setStatusCode(code);
        response.setMessage(message);
        return response;
    }
}
