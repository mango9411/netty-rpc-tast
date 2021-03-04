package com.mango.registry;

import com.mango.listener.NodeChangeListener;

import java.util.List;

/**
 * @author mango
 * @date 2021/3/4 20:00
 * @description: 注册中心
 */
public interface RpcRegistryHandler {

    /**
     * 服务注册
     *
     * @param service
     * @param ip
     * @param port
     * @return
     */
    boolean registry(String service, String ip, int port);

    /**
     * 服务发现
     *
     * @param service wo
     * @return
     */
    List<String> discovery(String service);

    /**
     * 添加监听者
     *
     * @param listener
     */
    void addListener(NodeChangeListener listener);

    /**
     * 注册中心销毁
     */
    void destroy();
}
