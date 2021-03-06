package com.mango.loadbalance;


import com.mango.client.RpcClient;

import java.util.List;
import java.util.Map;

/**
 * 负载均衡策略接口
 */
public interface LoadBalanceStrategy {

    /**
     * 获取请求客户端
     *
     * @param clientPool
     * @param serviceClassName
     * @return
     */
    RpcClient route(Map<String, List<RpcClient>> clientPool, String serviceClassName);
}