package com.mango.edu.rpc.registry.handler;

import com.mango.ConfigKeeper;
import com.mango.edu.rpc.registry.handler.impl.ZookeeperRegistryHandler;
import com.mango.registry.RpcRegistryHandler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Service;

/**
 * @author mango
 * @date 2021/3/4 20:55
 * @description:
 */
@Service
public class RpcRegistryFactory  implements FactoryBean<RpcRegistryHandler>, DisposableBean {
    private RpcRegistryHandler rpcRegistryHandler;

    @Override
    public RpcRegistryHandler getObject() throws Exception {
        if (null != rpcRegistryHandler) {
            return rpcRegistryHandler;
        }
        rpcRegistryHandler = new ZookeeperRegistryHandler(ConfigKeeper.getInstance().getZkAddr());
        return rpcRegistryHandler;
    }

    @Override
    public Class<?> getObjectType() {
        return RpcRegistryHandler.class;
    }

    @Override
    public void destroy() throws Exception {
        if (null != rpcRegistryHandler) {
            rpcRegistryHandler.destroy();
        }
    }
}
