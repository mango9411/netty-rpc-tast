package com.mango;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author mango
 * @date 2021/2/4 20:04
 * @description:
 */
@SpringBootApplication
public class ServerBoot {

    public static void main(String[] args) throws Exception {
        ConfigKeeper configKeeper = ConfigKeeper.getInstance();
        configKeeper.setPort(9000);
        configKeeper.setZkAddr("127.0.0.1:2181");
        configKeeper.setProviderSide(true);
        SpringApplication.run(ServerBoot.class, args);
    }
}
