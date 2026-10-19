package com.zxyy.user.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxyy.common.constant.MessageConstant;
import com.zxyy.user.mapper.AdminMapper;
import com.zxyy.user.domain.security.LoginUser;
import com.zxyy.user.domain.entity.Admin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //查询用户信息
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername,username);
        Admin admin = adminMapper.selectOne(wrapper);
        //没有查到用户直接抛出异常
        if(ObjectUtil.isNull(admin)){
            throw new UsernameNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        List<String> permission = adminMapper.getPermissionList(admin.getId());
        //封装为UserDetail对象
        return new LoginUser(admin,permission);
    }
}
