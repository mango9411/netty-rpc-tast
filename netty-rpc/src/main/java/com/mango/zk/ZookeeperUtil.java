package com.mango.zk;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author mango
 * @date 2021/2/22 20:47
 * @description:
 */
public class ZookeeperUtil {

    /**
     * 存放zookeeper记录的服务器地址
     */
    public static List<String> providerAddressList = new ArrayList<>();

    /**
     * 注册服务列表节点
     */
    public static String ZOOKEEPER_PATH = "/zdy-provider";

    /**
     * netty
     */
    public static Bootstrap bootstrap = new Bootstrap();

    /**
     * 创建会话
     *
     * @return
     */
    public static ZkClient createSession() {
        return new ZkClient("127.0.0.1:2181");
    }

    /**
     * 创建节点
     * 将本机地址注册到zookeeper
     */
    public static void createNote(String localAddress, ZkClient zkClient, boolean hashEphemeral) {
        if (zkClient == null) {
            zkClient = createSession();
        }
        System.out.println("会话创建完成");
        if (hashEphemeral) {
            //创建临时节点
            zkClient.createEphemeral(ZOOKEEPER_PATH + localAddress, localAddress);
        } else {
            //创建持久节点
            zkClient.createPersistent(ZOOKEEPER_PATH + localAddress, true);
        }
        System.out.println("地址注册完成");
    }

    /**
     * 删除节点
     */
    public void deleteNode(String nodePath) {
        ZkClient zkClient = createSession();
        System.out.println("会话创建完成");
        zkClient.deleteRecursive(nodePath);
        System.out.println("删除成功");
    }

    /**
     * 获取节点
     * 获取服务注册列表
     */
    public static List<String> getNoteChildren() {
        ZkClient zkClient = createSession();
        System.out.println("会话创建完成");
        //节点是否存在
        boolean exists = zkClient.exists(ZOOKEEPER_PATH);
        if (!exists) {
            //如果zookeeper注册节点不存在， 创建一个持久节点
            createNote("", zkClient, false);
        }
        registrationEvents(zkClient);
        providerAddressList = zkClient.getChildren(ZOOKEEPER_PATH);
        return providerAddressList;
    }

    /**
     * 注册事件
     *
     * @param zkClient
     */
    private static void registrationEvents(ZkClient zkClient) {
        //注册监听事件
        zkClient.subscribeChildChanges(ZOOKEEPER_PATH, new IZkChildListener() {
            @Override
            public void handleChildChange(String s, List<String> list) throws Exception {
                providerAddressList = list;
                System.out.println("节点数据发生变更， 变更后数据为：" + providerAddressList);
                connectServer(bootstrap, providerAddressList);
            }
        });
    }

    /**
     * 连接服务
     *
     * @param bootstrap
     * @param noteChildren
     * @throws InterruptedException
     */
    public static void connectServer(Bootstrap bootstrap, List<String> noteChildren) throws InterruptedException {
        for (String noteChild : noteChildren) {
            String[] split = noteChild.split(":");
            if (split.length > 0 || split != null) {
                bootstrap.connect(split[0], Integer.parseInt(split[1])).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if(channelFuture.isSuccess()){
                            System.out.println("host:" + split[0] + ", port: " + split[1] + "连接成功");
                        } else {
                            System.out.println("host:" + split[0] + ", port: " + split[1] + "连接失败");
                        }
                    }
                });
            }
        }
    }

    public static Bootstrap getBootstrap(){
        return bootstrap;
    }
}
