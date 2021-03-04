package com.mango.request;

import java.io.Serializable;

/**
 * @author mango
 * @date 2021/2/5 16:04
 * @description:
 */
public class RpcRequest implements Serializable {

    /**
     * 请求对象的ID
     */

    private String requestId;

    /**
     * 类名
     */

    private String className;

    /**
     * 方法名
     */

    private String methodName;

    /**
     * 参数类型
     */

    private Class<?>[] parameterTypes;

    /**
     * 入参
     */

    private Object[] parameters;

    public RpcRequest(){}

    public RpcRequest(String requestId, String className, String methodName, Class<?>[] parameterTypes, Object[] parameters) {
        this.requestId = requestId;
        this.className = className;
        this.methodName = methodName;
        this.parameters = parameters;
        this.parameterTypes = parameterTypes;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
}
