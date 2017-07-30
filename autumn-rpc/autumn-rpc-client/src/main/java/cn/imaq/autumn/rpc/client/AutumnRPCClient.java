package cn.imaq.autumn.rpc.client;

import cn.imaq.autumn.rpc.client.net.AutumnHttpClient;
import cn.imaq.autumn.rpc.client.net.AutumnHttpClientFactory;
import cn.imaq.autumn.rpc.client.proxy.AutumnProxy;
import cn.imaq.autumn.rpc.client.proxy.AutumnProxyFactory;
import cn.imaq.autumn.rpc.net.AutumnRPCRequest;
import cn.imaq.autumn.rpc.net.AutumnRPCResponse;
import cn.imaq.autumn.rpc.serialization.AutumnSerialization;
import cn.imaq.autumn.rpc.serialization.AutumnSerializationFactory;
import cn.imaq.autumn.rpc.util.PropertiesUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import static cn.imaq.autumn.rpc.net.AutumnRPCResponse.STATUS_OK;

@Slf4j
public class AutumnRPCClient {
    private static final String DEFAULT_CONFIG = "autumn-rpc-client-default.properties";

    private String host;
    private int port;
    private Properties config = new Properties();

    private AutumnHttpClient httpClient;
    private AutumnProxy proxy;
    private AutumnSerialization serialization;

    public AutumnRPCClient(String host, int port, String configFile, boolean useAutoConfig) {
        this.host = host;
        this.port = port;
        // load config from file
        try {
            PropertiesUtils.load(this.config, DEFAULT_CONFIG, configFile);
        } catch (IOException e) {
            log.error("Error loading config: " + e);
        }
        // auto config negotiation
        if (useAutoConfig) {
            log.info("Fetching config from server ...");
            try {
                JsonNode configJson = new ObjectMapper().readTree(new URL("http://" + host + ":" + port));
                log.info("Fetched " + configJson.size() + " config entries from server");
                for (Iterator<Map.Entry<String, JsonNode>> it = configJson.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    this.config.setProperty(entry.getKey(), entry.getValue().textValue());
                }
            } catch (IOException e) {
                log.error("Auto config error: " + e);
            }
        }
        // init fields
        this.httpClient = AutumnHttpClientFactory.getHttpClient(config.getProperty("autumn.httpclient"));
        log.info("Using http client: " + httpClient.getClass().getSimpleName());
        this.proxy = AutumnProxyFactory.getProxy(config.getProperty("autumn.proxy"));
        log.info("Using proxy: " + proxy.getClass().getSimpleName());
        this.serialization = AutumnSerializationFactory.getSerialization(config.getProperty("autumn.serialization"));
        log.info("Using serialization: " + serialization.getClass().getSimpleName());
    }

    public AutumnRPCClient(String host, int port) {
        this(host, port, null, false);
    }

    public AutumnRPCClient(String host, int port, String configFile) {
        this(host, port, configFile, false);
    }

    public AutumnRPCClient(String host, int port, boolean useAutoConfig) {
        this(host, port, null, useAutoConfig);
    }

    public <T> T getService(Class<T> interfaze) {
        return getService(interfaze, 3000);
    }

    public <T> T getService(Class<T> interfaze, int timeout) {
        return proxy.create(interfaze, (proxy, method, args) -> {
            String url = "http://" + host + ":" + port + "/" + interfaze.getName();
            AutumnRPCRequest request = AutumnRPCRequest.builder()
                    .methodName(method.getName())
                    .paramTypes(method.getParameterTypes())
                    .params(args)
                    .build();
            byte[] payload = serialization.serializeRequest(request);
            byte[] response = httpClient.post(url, payload, "application/json", timeout);
            AutumnRPCResponse rpcResponse = serialization.deserializeResponse(response, method.getReturnType());
            if (rpcResponse.getStatus() == STATUS_OK) {
                return rpcResponse.getResult();
            } else {
                throw (Throwable) rpcResponse.getResult();
            }
        });
    }
}