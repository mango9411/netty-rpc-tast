package com.mango.idle;

import com.mango.request.RpcRequest;

/**
 * @author mango
 * @date 2021/3/4 20:53
 * @description:
 */
public class Beat {
    public static final int BEAT_INTERVAL = 3;
    public static final String BEAT_ID = "BEAT_PING_PONG";

    public static RpcRequest BEAT_PING;

    static {
        BEAT_PING = new RpcRequest();
        BEAT_PING.setRequestId(BEAT_ID);
    }
}
