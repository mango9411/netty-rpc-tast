package com.mango.listener;

import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;

import java.util.List;

/**
 * @author mango
 * @date 2021/3/4 21:10
 * @description:
 */
public interface NodeChangeListener {
    /**
     * 节点变更时通知listener
     *
     * @param children
     * @param serviceList
     * @param pathChildrenCacheEvent
     */
    void notify(String children, List<String> serviceList, PathChildrenCacheEvent pathChildrenCacheEvent);
}
