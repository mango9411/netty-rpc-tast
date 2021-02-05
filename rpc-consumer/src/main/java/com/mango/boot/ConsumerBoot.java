package com.mango.boot;

import com.mango.client.RPCConsumer;
import com.mango.service.UserService;

/**
 * @author mango
 * @date 2021/2/4 20:59
 * @description:
 */
public class ConsumerBoot {


    public static void main(String[] args) throws InterruptedException {
        /**
         * 创建代理对象
         */
        UserService userService = (UserService) RPCConsumer.createProxy(UserService.class);

        /**
         * 循环给服务器写数据
         */
        while (true) {
            String sayHello = userService.sayHello("are you ok");
            System.out.println(sayHello);
            Thread.sleep(2000);
        }
    }
}
