package com.mango.handler;

import com.mango.request.RpcRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.Callable;

/**
 * @author mango
 * @date 2021/2/4 20:16
 * @description:
 */
public class UserClientHandler extends ChannelInboundHandlerAdapter implements Callable {

    /**
     * 定义成员变量 存储Handler信息，写操作  事件处理器上下文对象
     */
    private ChannelHandlerContext channelHandlerContext;

    /**
     * 服务器返回数据
     */
    private String result;

    /**
     * 发送给服务器的数据
     */
    private RpcRequest param;

    /**
     * 实现channelActive  客户端和服务器连接时该方法自动执行
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.channelHandlerContext = ctx;
    }

    /**
     * 实现channelRead  当我们读到服务器数据， 该方法自动执行
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public synchronized void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        this.result = msg.toString();
        notify();
    }

    /**
     * 将客户端的数据写到服务器
     *
     * @return
     * @throws Exception
     */
    @Override
    public synchronized Object call() throws Exception {
        channelHandlerContext.writeAndFlush(param);
        wait();
        return result;
    }

    /**
     * 设置参数
     *
     * @param param
     */
    public void setParam(RpcRequest param) {
        this.param = param;
    }
}
