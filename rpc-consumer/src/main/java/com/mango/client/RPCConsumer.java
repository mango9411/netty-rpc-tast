package com.mango.client;

import com.alibaba.fastjson.JSON;
import com.mango.decoder.RpcDecoder;
import com.mango.encoder.RpcEncoder;
import com.mango.handler.UserClientHandler;
import com.mango.request.RpcRequest;
import com.mango.serializer.JSONSerializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author mango
 * @date 2021/2/4 20:13
 * @description: 消费者
 */
public class RPCConsumer {

    /**
     * 创建线程池对象   处理自定义事件        根据CPU分配
     */
    private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * 声明一个自定义事件处理器， UserClientHandler
     */
    private static UserClientHandler userClientHandler;

    /**
     * 初始化客户端
     */
    public static void initClient() throws InterruptedException {
        userClientHandler = new UserClientHandler();

        NioEventLoopGroup group = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap().group(group);
        //设置通道为NIO
        bootstrap.channel(NioSocketChannel.class)
                //设置请求协议
                .option(ChannelOption.TCP_NODELAY, true)
                //监听channel 并初始化
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new RpcEncoder(RpcRequest.class, new JSONSerializer()));
                        pipeline.addLast(new StringDecoder());
                        pipeline.addLast(userClientHandler);
                    }
                });
        bootstrap.connect("127.0.0.1", 8999).sync();
    }

    /**
     * 使用JDK动态代理创建对象
     *
     * @param serviceClass  接口类型， 根据那个接口生成字类代理对象
     * @return
     */
    public static Object createProxy(Class<?> serviceClass) {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{serviceClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (userClientHandler == null) {
                    initClient();
                }
                RpcRequest rpcRequest = new RpcRequest("123", method.getDeclaringClass().getName(),
                        method.getName(), method.getParameterTypes(), args);
                userClientHandler.setParam(rpcRequest);

                Object result = executorService.submit(userClientHandler).get();

                return result;
            }
        });
    }
}
