package com.mango.edu.rpc.registry.handler.impl;

import com.mango.ConfigKeeper;
import com.mango.listener.NodeChangeListener;
import com.mango.metrics.RequestMetrics;
import com.mango.registry.RpcRegistryHandler;
import org.apache.commons.collections4.MapUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author mango
 * @date 2021/3/4 20:05
 * @description:
 */
public class ZookeeperRegistryHandler implements RpcRegistryHandler {

    private static final String LG_EDU_RPC_ZK_ROOT = "/zdy-provider/";
    private static final String ZK_PATH_SPILTER = "/";
    private static final List<NodeChangeListener> listenerList = new ArrayList<>();
    private final String url;
    private String charset = "UTF-8";
    private CuratorFramework client;
    private volatile boolean closed;
    private List<String> serviceList = new CopyOnWriteArrayList<>();
    private static final ScheduledExecutorService REPORT_WORKER = Executors.newScheduledThreadPool(1);

    public ZookeeperRegistryHandler(String url) {
        int timeout = 5000;
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(url)
                .retryPolicy(new RetryNTimes(1, 1000))
                .connectionTimeoutMs(timeout)
                .sessionTimeoutMs(timeout);

        client = builder.build();
        client.getConnectionStateListenable().addListener((CuratorFramework curatorFramework, ConnectionState connectionState) -> {
            if (ConnectionState.CONNECTED.equals(connectionState)) {
                System.out.println("注册中心连接成功");
            }
        });
        // 启动
        client.start();
        this.url = url;

        // 定时上报
        ConfigKeeper instance = ConfigKeeper.getInstance();
        boolean consumerSide = instance.isConsumerSide();
        int interval = instance.getInterval();
        // 只有在消费者端才去上报耗时统计
        if (consumerSide && interval > 0) {
            REPORT_WORKER.scheduleWithFixedDelay(() -> {
                ConcurrentHashMap<String, Long> metricMap = RequestMetrics.getInstance().getMetricMap();
                if (MapUtils.isEmpty(metricMap)) {
                    return;
                }
                System.out.println("自动上报节点耗时日志:" + metricMap);
                metricMap.entrySet().forEach(entry -> {
                    String address = entry.getKey();
                    Long cost = entry.getValue();
                    // 先创建路径
                    String zkPath = metricsPath();
                    if (!exists(zkPath)) {
                        create(zkPath, false);
                    }
                    String instancePath = zkPath + ZK_PATH_SPILTER + address;

                    updateWithData(instancePath, cost.toString(), true);
                });
            }, interval, interval, TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean registry(String service, String ip, int port) {
        String zkPath = providerPath(service);
        if (!exists(zkPath)) {
            create(zkPath, false);
        }
        // /zdy-provider/com.lagou.edu.api.UserService/provider/127.0.0.1:8990
        String instancePath = zkPath + ZK_PATH_SPILTER + ip + ":" + port;
        create(instancePath, true);
        return true;
    }

    @Override
    public List<String> discovery(String service) {
        //完成服务地址的查找
        // /zdy-provider/com.lagou.edu.api.UserService/provider
        String path = providerPath(service);
        try {
            // 第一次为空，需要从zk获取，后续通过watcher机制更新
            if (CollectionUtils.isEmpty(serviceList)) {
                System.out.println("首次从注册中心查找服务地址...");
                serviceList = client.getChildren().forPath(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.registryWatch(service, path);
        return serviceList;
    }

    private void registryWatch(final String service, final String path) {
        PathChildrenCache nodeCache = new PathChildrenCache(client, path, true);
        try {
            nodeCache.getListenable().addListener((client, pathChildrenCacheEvent) -> {
                // 更新本地的缓
                serviceList = client.getChildren().forPath(path);
                listenerList.stream().forEach(nodeChangeListener -> {
                    System.out.println("节点变化，开始通知业务");
//                    nodeChangeListener.notify(service, serviceList, pathChildrenCacheEvent);
                });
            });
            // /lg-edu-rpc/com.lagou.edu.api.UserService/provider/127.0.0.1:8990
            /*
             * StartMode：初始化方式
             * POST_INITIALIZED_EVENT：异步初始化。初始化后会触发事件
             * NORMAL：异步初始化
             * BUILD_INITIAL_CACHE：同步初始化
             * */
            nodeCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addListener(NodeChangeListener listener) {
        listenerList.add(listener);
    }

    @Override
    public void destroy() {
        if (null != client) {
            client.close();
        }
    }

    /**
     * 创建永久节点
     *
     * @param path
     */
    public void createPersistent(String path) {
        try {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
        } catch (KeeperException.NodeExistsException e) {
            System.out.println("路径[" + path + "]已存在");
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * 创建临时节点
     *
     * @param path
     */
    public void createEphemeral(String path) {
        try {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
        } catch (KeeperException.NodeExistsException e) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * 创建永久节点
     *
     * @param path
     * @param data
     */
    protected void createPersistent(String path, String data) {
        try {
            byte[] dataBytes = data.getBytes(charset);
            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path, dataBytes);
        } catch (KeeperException.NodeExistsException e) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * 创建临时节点
     *
     * @param path
     * @param data
     */
    protected void createEphemeral(String path, String data) {
        try {
            byte[] dataBytes = data.getBytes(charset);
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, dataBytes);
        } catch (KeeperException.NodeExistsException e) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected void update(String path, String data) {
        try {
            byte[] dataBytes = data.getBytes(charset);
            client.setData().forPath(path, dataBytes);
        } catch (KeeperException.NodeExistsException e) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void create(String path, boolean ephemeral) {
        if (ephemeral) {
            this.createEphemeral(path);
        } else {
            this.createPersistent(path);
        }
    }

    public void createWithData(String path, String content, boolean ephemeral) {
        if (ephemeral) {
            this.createEphemeral(path, content);
        } else {
            this.createPersistent(path, content);
        }
    }

    public void updateWithData(String path, String content, boolean ephemeral) {
        if (ephemeral) {
            if (exists(path)) {
                update(path, content);
            } else {
                this.createEphemeral(path, content);
            }
        } else {
            if (exists(path)) {
                update(path, content);
            } else {
                this.createPersistent(path, content);
            }
        }
    }

    public boolean isConnected() {
        return this.client.getZookeeperClient().isConnected();
    }

    public void close() {
        if (closed) {
            return;
        }
        this.closed = true;
        this.client.close();
    }


    public String getUrl() {
        return url;
    }

    public boolean exists(String path) {
        try {
            if (client.checkExists().forPath(path) != null) {
                return true;
            }
        } catch (KeeperException.NoNodeException e) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return false;
    }

    private String providerPath(String service) {
        return LG_EDU_RPC_ZK_ROOT + service + ZK_PATH_SPILTER + "provider";
    }

    private String metricsPath() {
        return LG_EDU_RPC_ZK_ROOT + "metrics";
    }
}
