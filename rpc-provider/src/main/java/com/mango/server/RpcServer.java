package com.mango.server;

import com.mango.ConfigKeeper;
import com.mango.decoder.RpcDecoder;
import com.mango.edu.rpc.registry.ProviderLoader;
import com.mango.edu.rpc.registry.handler.RpcRegistryFactory;
import com.mango.encoder.RpcEncoder;
import com.mango.handler.RpcServerHandler;
import com.mango.idle.Beat;
import com.mango.registry.RpcRegistryHandler;
import com.mango.request.RpcRequest;
import com.mango.response.RpcResponse;
import com.mango.serializer.JSONSerializer;
import com.mango.server.config.RpcServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author mango
 * @date 2021/3/4 20:43
 * @description:
 */
@Service
public class RpcServer implements InitializingBean, DisposableBean {
    @Autowired
    private RpcRegistryFactory registryFactory;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private RpcServerConfig config;

    public void startServer() throws Exception {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 心跳检测
                        pipeline.addLast(new IdleStateHandler(0, 0, Beat.BEAT_INTERVAL * 3, TimeUnit.SECONDS));
                        // 请求解码
                        pipeline.addLast(new RpcDecoder(RpcRequest.class, new JSONSerializer()));
                        // 响应编码
                        pipeline.addLast(new RpcEncoder(RpcResponse.class, new JSONSerializer()));
                        // 业务处理
                        pipeline.addLast(new RpcServerHandler());
                    }
                });
        String ip = "127.0.0.1";
        config.setIp(ip);
        int port = config.getPort();
        String applicationName = config.getApplicationName();

        ChannelFuture sync = serverBootstrap.bind(ip, port).sync();
        // 延迟注册
        if (config.getDelay() > 0) {
            Thread.sleep(config.getDelay());
        }
        System.out.println("=============开始注册=============");
        this.registry(ip, port, applicationName, config.getServices());
        System.out.println("=============启动成功, ip:" + ip + ", port:" + port + "=============");
        sync.channel().closeFuture().sync();
    }

    public void registry(String ip, int port, String applicationName, Map<String, Object> instanceMap) throws Exception {
        if (MapUtils.isEmpty(instanceMap)) {
            System.out.println("no service find");
            throw new RuntimeException("no service find");
        }
        RpcRegistryHandler registryHandler = registryFactory.getObject();
        if (null == registryHandler) {
            System.out.println("registryHandler is null");
            throw new RuntimeException("registryHandler is null");
        }
        instanceMap.entrySet().stream().forEach(stringObjectEntry -> registryHandler.registry(stringObjectEntry.getKey(), ip, port));
    }

    @Override
    public void destroy() throws Exception {
        if (null != bossGroup) {
            bossGroup.shutdownGracefully();
        }
        if (null != workerGroup) {
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Object> instanceCacheMap = ProviderLoader.getInstanceCacheMap();

        config = RpcServerConfig.builder()
                .applicationName("rpc-provider")
                .port(ConfigKeeper.getInstance().getPort())
                .delay(3000)
                .services(instanceCacheMap)
                .providerSide(true).build();

        startServer();
    }
}
