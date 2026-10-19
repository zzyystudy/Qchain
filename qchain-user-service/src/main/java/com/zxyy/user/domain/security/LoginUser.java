package com.zxyy.user.domain.security;

import com.zxyy.user.domain.entity.Admin;
import com.zxyy.user.constant.StatusConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
public class LoginUser implements UserDetails {

    private Admin admin;

    private List<String> permission;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permission == null ? List.of() : permission.stream()
                .filter(Objects::nonNull).filter(value -> !value.isBlank())
                .map(SimpleGrantedAuthority::new).toList();
    }

    public Long getUserId() {
        return admin.getId();
    }

    @Override
    public String getPassword() {
        return admin.getPassword();
    }

    @Override
    public String getUsername() {
        return admin.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return StatusConstant.ENABLE.equals(admin.getStatus());
    }
}
