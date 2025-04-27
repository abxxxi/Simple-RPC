package com.simplerpc.transport.netty.codec;

import com.simplerpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC编码器，用于将对象编码为网络字节流
 */
@Slf4j
public class RpcEncoder extends MessageToByteEncoder<Object> {

    private final Serializer serializer;
    private final Class<?> clazz;

    /**
     * 构造函数
     *
     * @param serializer 序列化器
     * @param clazz 目标类型
     */
    public RpcEncoder(Serializer serializer, Class<?> clazz) {
        this.serializer = serializer;
        this.clazz = clazz;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {
        // 检查消息类型
        if (clazz.isInstance(msg)) {
            try {
                // 序列化为字节数组
                byte[] bytes = serializer.serialize(msg);

                // 写入消息长度
                out.writeInt(bytes.length);
                // 写入消息体
                out.writeBytes(bytes);
            } catch (Exception e) {
                log.error("序列化数据失败", e);
                throw new RuntimeException("序列化数据失败", e);
            }
        }
    }
}
