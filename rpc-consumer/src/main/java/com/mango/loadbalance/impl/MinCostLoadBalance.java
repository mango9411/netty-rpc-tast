package com.mango.loadbalance.impl;

import com.mango.client.RpcClient;
import com.mango.loadbalance.AbstractLoadBalance;
import com.mango.metrics.RequestMetrics;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 最小耗时负载策略
 */
public class MinCostLoadBalance extends AbstractLoadBalance {

    @Override
    protected RpcClient doSelect(List<RpcClient> clients) {
        List<RequestMetrics.Metrics> allInstances = RequestMetrics.getInstance().getAllInstances();
        if (CollectionUtils.isEmpty(allInstances)) {
            // 随机返回一个
            Random random = new Random();
            return clients.get(random.nextInt(clients.size()));
        }
        Collections.sort(allInstances);
        RequestMetrics.Metrics metrics0 = allInstances.get(0);
        if (allInstances.size() == 1) {
            return clients.stream().filter(rpcClient -> rpcClient.getIp().equals(metrics0.getIp()) &&
                    (rpcClient.getPort() == metrics0.getPort())).collect(Collectors.toList()).get(0);
        }
        RequestMetrics.Metrics metrics1 = allInstances.get(1);
        if (metrics0.getCost().equals(metrics1.getCost())) {
            // 随机返回一个
            Random random = new Random();
            return clients.get(random.nextInt(2));
        }
        try {
            return clients.stream().filter(rpcClient -> rpcClient.getIp().equals(metrics0.getIp()) &&
                    (rpcClient.getPort() == metrics0.getPort())).collect(Collectors.toList()).get(0);
        } catch (IndexOutOfBoundsException e) {
            Random random = new Random();
            return clients.get(random.nextInt(clients.size()));
        }
    }
}