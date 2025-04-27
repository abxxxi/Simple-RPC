package com.simplerpc.serialize;

/**
 * 序列化接口，定义了序列化和反序列化的方法
 */
public interface Serializer {
    /**
     * 序列化方法，将对象转换为字节数组
     *
     * @param obj 要序列化的对象
     * @return 序列化后的字节数组
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化方法，将字节数组转换为对象
     *
     * @param bytes 要反序列化的字节数组
     * @param clazz 目标类型
     * @param <T> 目标类型
     * @return 反序列化后的对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
