package com.mango.loadbalance.impl;


import com.mango.client.RpcClient;
import com.mango.loadbalance.AbstractLoadBalance;

import java.util.List;
import java.util.Random;

/**
 * 随机负载策略
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    @Override
    protected RpcClient doSelect(List<RpcClient> t) {
        int length = t.size();
        Random random = new Random();
        return t.get(random.nextInt(length));
    }
}