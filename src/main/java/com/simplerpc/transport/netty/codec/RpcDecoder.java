package com.simplerpc.transport.netty.codec;

import com.simplerpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * RPC解码器，用于将网络字节流解码为对象
 */
@Slf4j
public class RpcDecoder extends ByteToMessageDecoder {

    private final Serializer serializer;
    private final Class<?> clazz;

    /**
     * 构造函数
     *
     * @param serializer 序列化器
     * @param clazz 目标类型
     */
    public RpcDecoder(Serializer serializer, Class<?> clazz) {
        this.serializer = serializer;
        this.clazz = clazz;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 消息头：4字节整数表示消息体长度
        if (in.readableBytes() < 4) {
            return;
        }

        // 记录当前读取位置
        in.markReaderIndex();

        // 读取消息长度
        int length = in.readInt();

        // 如果消息体长度为负数，说明数据异常
        if (length < 0) {
            ctx.close();
            return;
        }

        // 如果消息体不完整，重置读取位置
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        // 读取消息体
        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        try {
            // 反序列化为对象
            Object obj = serializer.deserialize(bytes, clazz);
            out.add(obj);
        } catch (Exception e) {
            log.error("反序列化数据失败", e);
            throw new RuntimeException("反序列化数据失败", e);
        }
    }
}
