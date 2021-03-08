package com.mango.client;

import com.mango.encoder.RpcEncoder;
import com.mango.handler.UserClientHandler;
import com.mango.listener.NodeChangeListener;
import com.mango.loadbalance.LoadBalanceStrategy;
import com.mango.loadbalance.impl.MinCostLoadBalance;
import com.mango.metrics.RequestMetrics;
import com.mango.registry.RpcRegistryHandler;
import com.mango.request.RpcRequest;
import com.mango.serializer.JSONSerializer;
import com.mango.zk.ZookeeperUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.prefs.NodeChangeEvent;

/**
 * @author mango
 * @date 2021/2/4 20:13
 * @description: 消费者
 */
public class RPCConsumer implements NodeChangeListener {

    private static final Map<String, List<RpcClient>> CLIENT_POOL = new ConcurrentHashMap<>();
    private RpcRegistryHandler rpcRegistryHandler;
    private Map<String, Object> serviceMap;
    private LoadBalanceStrategy loadBalance = new MinCostLoadBalance();

    /**
     * 创建线程池对象   处理自定义事件        根据CPU分配
     */
    private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * 声明一个自定义事件处理器， UserClientHandler
     */
    private static UserClientHandler userClientHandler;


    /**
     * 初始化
     *
     * @param rpcRegistryHandler
     * @param serviceMap
     */
    public RPCConsumer(RpcRegistryHandler rpcRegistryHandler, Map<String, Object> serviceMap) {
        this.rpcRegistryHandler = rpcRegistryHandler;
        this.serviceMap = serviceMap;

        // 开始自动注册消费者逻辑
        serviceMap.entrySet().forEach(new Consumer<Map.Entry<String, Object>>() {
            @Override
            public void accept(Map.Entry<String, Object> entry) {
                String interfaceName = entry.getKey();
                List<String> discovery = rpcRegistryHandler.discovery(interfaceName);

                List<RpcClient> rpcClients = CLIENT_POOL.get(interfaceName);
                if (CollectionUtils.isEmpty(rpcClients)) {
                    rpcClients = new ArrayList<>();
                }
                for (String item : discovery) {
                    String[] split = item.split(":");
                    RpcClient rpcClient = new RpcClient(split[0], Integer.parseInt(split[1]));
                    try {
                        rpcClient.initClient(interfaceName);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    rpcClients.add(rpcClient);
                    CLIENT_POOL.put(interfaceName, rpcClients);
                }
            }
        });
        rpcRegistryHandler.addListener(this);
    }

    /**
     * 初始化客户端
     */
    public static void initClient() throws InterruptedException {
        userClientHandler = new UserClientHandler();

        NioEventLoopGroup group = new NioEventLoopGroup();

        Bootstrap bootstrap = ZookeeperUtil.getBootstrap().group(group);
        //设置通道为NIO
        bootstrap.channel(NioSocketChannel.class)
                //设置请求协议
                .option(ChannelOption.TCP_NODELAY, true)
                //监听channel 并初始化
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new RpcEncoder(RpcRequest.class, new JSONSerializer()));
                        pipeline.addLast(new StringDecoder());
                        pipeline.addLast(userClientHandler);
                    }
                });
        List<String> noteChildren = ZookeeperUtil.getNoteChildren();
        ZookeeperUtil.connectServer(bootstrap, noteChildren);
    }

    /**
     * 使用JDK动态代理创建对象
     *
     * @param serviceClass 接口类型， 根据那个接口生成字类代理对象
     * @return
     */
    public Object createProxy(Class<?> serviceClass) {
        //借助JDK动态代理生成代理对象
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{serviceClass}, (proxy, method, args) -> {

            //（1）调用初始化netty客户端的方法
            String serviceClassName = serviceClass.getName();

            // 封装request对象
            RpcRequest request = new RpcRequest();
            String requestId = UUID.randomUUID().toString();

            String className = method.getDeclaringClass().getName();
            String methodName = method.getName();

            Class<?>[] parameterTypes = method.getParameterTypes();

            request.setRequestId(requestId);
            request.setClassName(className);
            request.setMethodName(methodName);
            request.setParameterTypes(parameterTypes);
            request.setParameters(args);

            // 打印request
            System.out.println("请求内容: " + request);

            // 去服务端请求数据
            RpcClient rpcClient = loadBalance.route(CLIENT_POOL, serviceClassName);
            if (null == rpcClient) {
                return null;
            }
            try {
                return rpcClient.send(request);
            } catch (Exception e) {
                if (e.getClass().getName().equals("java.nio.channels.ClosedChannelException")) {
                    System.out.println("发送发生异常, 稍后重试:" + e.getMessage());
                    e.printStackTrace();
                    Thread.sleep(3000);
                    RpcClient otherRpcClient = loadBalance.route(CLIENT_POOL, serviceClassName);
                    if (null == otherRpcClient) {
                        return null;
                    }
                    return otherRpcClient.send(request);
                }
                throw e;
            }
        });
    }

    @Override
    public void notify(String service, List<String> serviceList, PathChildrenCacheEvent pathChildrenCacheEvent) {
        List<RpcClient> rpcClients = CLIENT_POOL.get(service);
        PathChildrenCacheEvent.Type eventType = pathChildrenCacheEvent.getType();
        System.out.println("收到节点变更通知:" + eventType + "----" + rpcClients + "---" + service + "---" + serviceList);
        String path = pathChildrenCacheEvent.getData().getPath();
        String instanceConfig = path.substring(path.lastIndexOf("/") + 1);

        // 增加节点
        if (PathChildrenCacheEvent.Type.CHILD_ADDED.equals(eventType)
                || PathChildrenCacheEvent.Type.CONNECTION_RECONNECTED.equals(eventType)) {
            if (CollectionUtils.isEmpty(rpcClients)) {
                rpcClients = new ArrayList<>();
            }
            String[] address = instanceConfig.split(":");
            RpcClient client = new RpcClient(address[0], Integer.parseInt(address[1]));
            try {
                client.initClient(service);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            rpcClients.add(client);

            // 节点耗时统计
            RequestMetrics.getInstance().addNode(address[0], Integer.parseInt(address[1]));
            System.out.println("新增节点:" + instanceConfig);
        } else if (PathChildrenCacheEvent.Type.CHILD_REMOVED.equals(eventType)
                || PathChildrenCacheEvent.Type.CONNECTION_SUSPENDED.equals(eventType)
                || PathChildrenCacheEvent.Type.CONNECTION_LOST.equals(eventType)) {
            // 移除节点
            if (CollectionUtils.isNotEmpty(rpcClients)) {
                String[] address = instanceConfig.split(":");
                for (int i = 0; i < rpcClients.size(); i++) {
                    RpcClient item = rpcClients.get(i);
                    if (item.getIp().equalsIgnoreCase(address[0]) && Integer.parseInt(address[1]) == item.getPort()) {
                        rpcClients.remove(item);
                        System.out.println("移除节点:" + instanceConfig);
                        RequestMetrics.getInstance().removeNode(address[0], Integer.parseInt(address[1]));
                    }
                }
            }
        }
    }
}
