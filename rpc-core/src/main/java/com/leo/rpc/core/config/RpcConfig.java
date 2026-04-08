package com.leo.rpc.core.config;

import com.leo.rpc.common.exception.RpcException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * RPC configuration model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcConfig {

    private static final String DEFAULT_RESOURCE = "application.properties";

    private static final String KEY_NACOS_ADDRESS = "rpc.nacos.address";
    private static final String KEY_NACOS_NAMESPACE = "rpc.nacos.namespace";
    private static final String KEY_SERVER_PORT = "rpc.server.port";
    private static final String KEY_SERVER_HOST = "rpc.server.host";
    private static final String KEY_SERIALIZER = "rpc.serializer";
    private static final String KEY_LOAD_BALANCE = "rpc.client.load-balance";
    private static final String KEY_BOSS_THREADS = "rpc.netty.boss-threads";
    private static final String KEY_WORKER_THREADS = "rpc.netty.worker-threads";
    private static final String KEY_REQUEST_TIMEOUT = "rpc.client.request-timeout";

    /**
     * Nacos server address.
     */
    @Builder.Default
    private String nacosAddress = "127.0.0.1:8848";

    /**
     * Nacos namespace.
     */
    @Builder.Default
    private String nacosNamespace = "public";

    /**
     * Server listen port.
     */
    @Builder.Default
    private int serverPort = 9090;

    /**
     * Server host address.
     */
    @Builder.Default
    private String serverHost = "127.0.0.1";

    /**
     * Serializer name.
     */
    @Builder.Default
    private String serializer = "kryo";

    /**
     * Load balancer strategy.
     */
    @Builder.Default
    private String loadBalance = "random";

    /**
     * Netty boss thread count.
     */
    @Builder.Default
    private int bossThreads = 1;

    /**
     * Netty worker thread count.
     */
    @Builder.Default
    private int workerThreads = 0; // 0 = default (CPU * 2)

    /**
     * Request timeout in milliseconds.
     */
    @Builder.Default
    private long requestTimeout = 5000;

    public static RpcConfig loadFromClasspath() {
        return loadFromClasspath(DEFAULT_RESOURCE);
    }

    public static RpcConfig loadFromClasspath(String resourceName) {
        return fromProperties(loadProperties(resourceName));
    }

    public static RpcConfig fromProperties(Properties properties) {
        RpcConfig defaults = new RpcConfig();
        return RpcConfig.builder()
                .nacosAddress(getString(properties, KEY_NACOS_ADDRESS, defaults.getNacosAddress()))
                .nacosNamespace(getString(properties, KEY_NACOS_NAMESPACE, defaults.getNacosNamespace()))
                .serverPort(getInt(properties, KEY_SERVER_PORT, defaults.getServerPort()))
                .serverHost(getString(properties, KEY_SERVER_HOST, defaults.getServerHost()))
                .serializer(getString(properties, KEY_SERIALIZER, defaults.getSerializer()))
                .loadBalance(getString(properties, KEY_LOAD_BALANCE, defaults.getLoadBalance()))
                .bossThreads(getInt(properties, KEY_BOSS_THREADS, defaults.getBossThreads()))
                .workerThreads(getInt(properties, KEY_WORKER_THREADS, defaults.getWorkerThreads()))
                .requestTimeout(getLong(properties, KEY_REQUEST_TIMEOUT, defaults.getRequestTimeout()))
                .build();
    }

    private static Properties loadProperties(String resourceName) {
        Properties properties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = RpcConfig.class.getClassLoader();
        }
        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream != null) {
                properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RpcException("Failed to load rpc configuration from " + resourceName, e);
        }
        return properties;
    }

    private static String getString(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private static int getInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    private static long getLong(Properties properties, String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Long.parseLong(value.trim());
    }
}
