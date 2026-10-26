package com.zxyy.common.security;

/** 由网关覆盖写入，浏览器提供的同名请求头不能作为身份依据。 */
public final class IdentityHeaders {
    public static final String USER_ID = "X-Qchain-User-Id";
    public static final String GATEWAY_TOKEN = "X-Qchain-Gateway-Token";
    public static final String LOGIN_KEY_PREFIX = "qchain:user:login:";

    private IdentityHeaders() {}
}
