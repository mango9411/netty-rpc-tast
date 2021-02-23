package com.mango;

import com.mango.service.UserServiceImpl;
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
        SpringApplication.run(ServerBoot.class, args);
        UserServiceImpl.start("127.0.0.1", 9000);
    }
}
