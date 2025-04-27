package com.simplerpc.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * URL类，用于封装服务地址信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class URL implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口号
     */
    private int port;

    /**
     * 获取完整地址
     */
    public String getAddress() {
        return host + ":" + port;
    }

    /**
     * 从字符串解析URL
     */
    public static URL parse(String address) {
        String[] parts = address.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid address format: " + address);
        }
        return new URL(parts[0], Integer.parseInt(parts[1]));
    }
}
