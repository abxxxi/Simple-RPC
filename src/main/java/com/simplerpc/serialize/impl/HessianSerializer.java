package com.simplerpc.serialize.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.simplerpc.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Hessian序列化实现，性能比JSON更好，二进制格式
 */
@Slf4j
public class HessianSerializer implements Serializer {
    @Override
    public byte[] serialize(Object obj) {
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
            Hessian2Output output = new Hessian2Output(bos);
            output.writeObject(obj);
            output.flush();
            return bos.toByteArray();
        }catch (IOException e){
            log.error("Hessian序列化失败",e);
            throw new RuntimeException("Hessian序列化失败",e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try(ByteArrayInputStream bis = new ByteArrayInputStream(bytes)){
            Hessian2Input input = new Hessian2Input(bis);
            return (T) input.readObject(clazz);
        }catch (IOException e){
            log.error("Hessian反序列化失败",e);
            throw new RuntimeException("Hessian反序列化失败",e);
        }
    }
}
