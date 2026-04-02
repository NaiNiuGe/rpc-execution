package com.leo.rpc.core.serialize;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.leo.rpc.common.exception.RpcException;
import com.leo.rpc.common.model.RpcRequest;
import com.leo.rpc.common.model.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Kryo序列化实现
 * <p>
 * Kryo是非线程安全的，使用ThreadLocal为每个线程分配独立的Kryo实例
 */
public class KryoSerializer implements Serializer {

    private static final Logger log = LoggerFactory.getLogger(KryoSerializer.class);

    /**
     * ThreadLocal保证Kryo线程安全
     */
    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        // 注册常用类以提高序列化性能
        kryo.register(RpcRequest.class);
        kryo.register(RpcResponse.class);
        kryo.register(Class[].class);
        kryo.register(Object[].class);
        kryo.register(Class.class);
        // 关闭注册要求，允许序列化未注册的类
        kryo.setRegistrationRequired(false);
        // 开启引用检测，支持循环引用
        kryo.setReferences(true);
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Output output = new Output(baos)) {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            kryo.writeObject(output, obj);
            output.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Kryo序列化失败", e);
            throw new RpcException("Kryo序列化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             Input input = new Input(bais)) {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            return kryo.readObject(input, clazz);
        } catch (Exception e) {
            log.error("Kryo反序列化失败", e);
            throw new RpcException("Kryo反序列化失败: " + e.getMessage(), e);
        }
    }
}
