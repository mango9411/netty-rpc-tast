package com.mango.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author mango
 * @date 2021/3/4 20:52
 * @description:
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcService {

    /**
     * 服务版本号，待实现
     *
     * @return
     */
    String version() default "";

    /**
     * 超时时间，待实现
     *
     * @return
     */
    int timeout() default -1;
}
