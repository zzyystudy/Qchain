package com.zxyy.user.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxyy.common.result.Result;
import com.zxyy.common.context.BaseContext;
import com.zxyy.common.constant.MessageConstant;
import com.zxyy.user.constant.JwtClaimsConstant;
import com.zxyy.user.domain.security.LoginUser;
import com.zxyy.common.security.LoginSession;
import com.zxyy.user.exception.PasswordError;
import com.zxyy.common.security.JwtProperties;
import com.zxyy.common.security.JwtUtil;
import com.zxyy.common.security.LoginSessionStore;
import com.zxyy.api.dto.user.AdminLoginDTO;
import com.zxyy.user.domain.entity.Admin;
import com.zxyy.api.vo.user.AdminLoginVo;
import com.zxyy.user.mapper.AdminMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminService extends ServiceImpl<AdminMapper, Admin> {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Resource
    private JwtProperties jwtProperties;
    @Autowired
    private LoginSessionStore sessions;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 登录接口
     * @param adminLoginDTO
     * @return
     */
    public Result<AdminLoginVo> login(AdminLoginDTO adminLoginDTO) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(adminLoginDTO.getUsername(),adminLoginDTO.getPassword());

        Authentication authenticate = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        //没通过为null
        if(ObjectUtil.isNull(authenticate)){
            throw new PasswordError(MessageConstant.PASSWORD_ERROR);
        }
        //认证通过发放token
        LoginUser loginUser = (LoginUser)authenticate.getPrincipal();
        Admin admin = loginUser.getAdmin();
        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID,admin.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        AdminLoginVo adminLoginVo = AdminLoginVo.builder()
                .id(admin.getId())
                .userName(admin.getUsername())
                .name(admin.getName())
                .token(token)
                .build();
        LoginSession session = new LoginSession(admin.getId(), admin.getUsername(),
                loginUser.getAuthorities().stream().map(authority -> authority.getAuthority()).toList());
        sessions.save(session,
                Duration.ofMillis(jwtProperties.getAdminTtl()));
        return Result.success(adminLoginVo);
    }

    public Result<String> logout() {
        //从securityContextHolder中获取用户id
        Long id = BaseContext.getCurrentId();
        //删除redis中用户的信息
        sessions.deleteByUserId(id);
        return Result.success("注销成功");
    }

    public Result<String> add(Admin admin) {
        String encode = passwordEncoder.encode(admin.getPassword());
        admin.setPassword(encode);
        save(admin);
        return Result.success();
    }
}
