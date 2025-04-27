package com.simplerpc.serialize.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplerpc.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * JSON序列化实现，基于Jackson库
 */
@Slf4j
public class JsonSerializer implements Serializer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public byte[] serialize(Object obj) {
        try{
            return objectMapper.writeValueAsBytes(obj);
        }catch (JsonProcessingException e){
            log.error("JSON序列化失败",e);
            throw new RuntimeException("JSON序列化失败",e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try{
            return objectMapper.readValue(bytes,clazz);
        }catch (IOException e){
            log.error("JSON反序列化失败",e);
            throw new RuntimeException("JSON反序列化失败",e);
        }
    }
}
