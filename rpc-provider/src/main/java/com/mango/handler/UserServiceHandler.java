package com.mango.handler;

import com.mango.request.RpcRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author mango
 * @date 2021/2/4 19:58
 * @description: 自定义业务处理器
 */
@Component
public class UserServiceHandler extends ChannelInboundHandlerAdapter implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        UserServiceHandler.context = applicationContext;
    }

    /**
     * 当客户端读取数据时，该方法会被调用
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //客户端发送请求会发传递一个参数：RpcRequest#sayHello#RpcRequest{}
        //判断当前请求是否符合规则
        RpcRequest rpcRequest = (RpcRequest) msg;

        Class<?> aClass = Class.forName(rpcRequest.getClassName());

        Object bean = context.getBean(aClass);

        Class<?> service = bean.getClass();

        for (Method method : service.getMethods()) {
            if (method.getName().equals(rpcRequest.getMethodName())) {
                Object[] parameters = rpcRequest.getParameters();
                method.invoke(bean, parameters);

                ctx.writeAndFlush("success");
            }
        }
    }
}
