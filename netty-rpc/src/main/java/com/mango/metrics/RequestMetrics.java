package com.mango.metrics;

import com.mango.ConfigKeeper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mango
 * @date 2021/3/4 20:58
 * @description:
 */
public class RequestMetrics {
    // 节点--最后一次耗时（毫秒）
    private static final ConcurrentHashMap<String, Long> COST_TIME_MAP = new ConcurrentHashMap<>();
    // 请求id--耗时对象
    private static final ConcurrentHashMap<String, Metrics> REQUEST_ID_MAP = new ConcurrentHashMap<>();
    private static volatile RequestMetrics requestMetrics;

    private RequestMetrics() {
    }

    /**
     * 获取统计数据Map
     *
     * @return
     */
    public ConcurrentHashMap<String, Long> getMetricMap() {
        return COST_TIME_MAP;
    }

    /**
     * 全局单例
     *
     * @return
     */
    public static RequestMetrics getInstance() {
        if (null == requestMetrics) {
            synchronized (ConfigKeeper.class) {
                requestMetrics = new RequestMetrics();
            }
        }
        return requestMetrics;
    }

    /**
     * 增加节点
     *
     * @param ip
     * @param port
     */
    public void addNode(String ip, int port) {
        COST_TIME_MAP.put(ip + ":" + port, 0L);
    }

    /**
     * 删除节点
     *
     * @param ip
     * @param port
     */
    public void removeNode(String ip, int port) {
        COST_TIME_MAP.remove(ip + ":" + port);
    }

    /**
     * 在客户端接到响应时，根据requestId计算耗时
     */
    public void calculate(String requestId) {
        Metrics metrics = REQUEST_ID_MAP.get(requestId);
        long cost = System.currentTimeMillis() - metrics.getStart();
        COST_TIME_MAP.put(metrics.getIp() + ":" + metrics.getPort(), cost);
        REQUEST_ID_MAP.remove(requestId);
    }

    /**
     * 请求时放入
     */
    public void put(String ip, int port, String requestId) {
        REQUEST_ID_MAP.put(requestId, new Metrics(ip, port, System.currentTimeMillis(), null));
    }

    /**
     * 获取所有节点耗时统计
     *
     * @return
     */
    public List<Metrics> getAllInstances() {
        List<Metrics> result = new ArrayList<>();
        COST_TIME_MAP.forEach((s, aLong) -> {
            String[] split = s.split(":");
            result.add(new Metrics(split[0], Integer.parseInt(split[1]), aLong));
        });
        return result;
    }

    /**
     * 数据统计类
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Metrics implements Comparable<Metrics> {
        private String ip;
        private int port;
        private Long start;
        private Long cost;

        public Metrics(String ip, int port, Long cost) {
            this.ip = ip;
            this.port = port;
            this.cost = cost;
        }

        @Override
        public int compareTo(Metrics o) {
            return getCost().compareTo(o.getCost());
        }
    }
}
