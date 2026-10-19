package com.zxyy.common.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.security.Principal;
import java.util.List;

/** Redis 中只保存登录身份和权限，不保存管理员密码及其他个人资料。 */
public record LoginSession(Long userId, String username, List<String> permissions)
        implements Principal {
    public LoginSession {
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }

    @JsonIgnore
    @Override
    public String getName() {
        return username;
    }
}
