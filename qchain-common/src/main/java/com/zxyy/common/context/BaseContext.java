package com.zxyy.common.context;

import com.zxyy.common.security.LoginSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 只读取当前服务已经认证的身份，由 Security 过滤器链管理上下文生命周期。 */
public final class BaseContext {
    private BaseContext() {
    }

    public static LoginSession getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof LoginSession session)
                || session.userId() == null) {
            throw new AuthenticationCredentialsNotFoundException("用户未登录");
        }
        return session;
    }

    public static Long getCurrentId() {
        return getCurrentUser().userId();
    }

    /** 保留商品服务已有的方法调用，统一从 Security 上下文读取用户 ID。 */
    public static Long getSecurityContextHolderUserId() {
        return getCurrentId();
    }
}
