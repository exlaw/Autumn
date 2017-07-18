package cn.imaq.autumn.rpc.server;

import cn.imaq.autumn.rpc.server.net.AutumnHttpServer;
import cn.imaq.autumn.rpc.server.util.AutumnRPCBanner;
import cn.imaq.autumn.rpc.server.util.ConfigUtil;
import cn.imaq.autumn.rpc.server.util.LogUtil;
import org.rapidoid.net.Server;

public class AutumnRPCServer {
    private static final AutumnHttpServer httpServer = new AutumnHttpServer();
    private static Server listeningServer;

    public static void start() {
        start(null);
    }

    public static void start(String configFile) {
        AutumnRPCBanner.printBanner();
        ConfigUtil.loadConfig(configFile);
        synchronized (httpServer) {
            stop();
            String host = ConfigUtil.get("http.host");
            Integer port = Integer.valueOf(ConfigUtil.get("http.port"));
            LogUtil.I("Starting HTTP server on " + host + ":" + port);
            listeningServer = httpServer.listen(host, port);
        }
    }

    public static void stop() {
        synchronized (httpServer) {
            if (listeningServer != null && listeningServer.isActive()) {
                listeningServer.shutdown();
            }
        }
    }

    public static void main(String[] args) {
        start();
    }
}