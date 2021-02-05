package com.mango.service;

import com.mango.decoder.RpcDecoder;
import com.mango.encoder.RpcEncoder;
import com.mango.handler.UserServiceHandler;
import com.mango.request.RpcRequest;
import com.mango.serializer.JSONSerializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * @author mango
 * @date 2021/2/4 19:51
 * @description:
 */
@Service
public class UserServiceImpl implements UserService {

    @Override
    public String sayHello(String msg) {
        System.out.println("are you ok?" + msg);
        return "success";
    }

    /**
     * 创建一个方法启动服务器
     *
     * @throws Exception
     */
    public static void start(String ip, int port) throws Exception {
        //负责接收请求
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        //负责读写
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        //启动引导类
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        //设置线程池
        serverBootstrap.group(bossGroup, workerGroup);

        serverBootstrap.channel(NioServerSocketChannel.class)
                //创建监听channel
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        //获取管道对象
                        ChannelPipeline pipeline = nioSocketChannel.pipeline();
                        pipeline.addLast(new StringEncoder());
                        pipeline.addLast(new RpcDecoder(RpcRequest.class, new JSONSerializer()));
                        //把自定义的channelHandler对象添加到通道中
                        pipeline.addLast(new UserServiceHandler());
                    }
                });
        //绑定端口
        serverBootstrap.bind(ip, port).sync();
        System.out.println("server start success....");
    }
}
