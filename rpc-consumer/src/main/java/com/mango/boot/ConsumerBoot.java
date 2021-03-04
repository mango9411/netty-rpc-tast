package com.mango.boot;

import com.mango.ConfigKeeper;
import com.mango.client.RPCConsumer;
import com.mango.edu.rpc.registry.handler.impl.ZookeeperRegistryHandler;
import com.mango.registry.RpcRegistryHandler;
import com.mango.service.UserService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mango
 * @date 2021/2/4 20:59
 * @description:
 */
public class ConsumerBoot {


    public static void main(String[] args) throws InterruptedException {
        Map<String, Object> instanceCacheMap = new HashMap<>();
        instanceCacheMap.put(UserService.class.getName(), UserService.class);

        ConfigKeeper.getInstance().setConsumerSide(true);
        // 启动一个定时的线程池，每隔xx秒开始自动上报统计数据到注册中心
        ConfigKeeper.getInstance().setInterval(5);

        RpcRegistryHandler rpcRegistryHandler = new ZookeeperRegistryHandler("127.0.0.1:2181");
        RPCConsumer consumer = new RPCConsumer(rpcRegistryHandler, instanceCacheMap);

        UserService userService = (UserService) consumer.createProxy(UserService.class);

        while (true) {
            Thread.sleep(2000);
            userService.sayHello("are you ok?");
        }
    }
}
