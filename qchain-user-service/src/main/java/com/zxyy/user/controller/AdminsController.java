package com.zxyy.user.controller;


import com.zxyy.user.constant.JwtClaimsConstant;
import com.zxyy.api.dto.user.AdminLoginDTO;
import com.zxyy.user.domain.entity.Admin;
import com.zxyy.api.vo.user.AdminLoginVo;
import com.zxyy.user.properties.JwtProperties;
import com.zxyy.common.result.Result;
import com.zxyy.user.service.AdminService;
import com.zxyy.user.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/admins")
@Tag(name = "管理端用户接口")
@Slf4j
public class AdminsController {

    @Autowired
    private AdminService adminService;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "管理员登录")
    public Result<AdminLoginVo> login(@RequestBody AdminLoginDTO employeeLoginDTO) {
        log.info("管理员登录请求");
        return adminService.login(employeeLoginDTO);
    }

    @GetMapping("logout")
    @Operation(summary = "管理员退出登录接口")
    public Result<String> logout(){
        return adminService.logout();
    }


    @GetMapping("/hello")
    @Operation(description = "hello")
    @PreAuthorize("hasAnyAuthority('system:admin')")
    public Result<String> hello(){
        return Result.success("hello");
    }

    @PostMapping("/add")
    @Operation(summary = "新增管理员")
    @PreAuthorize("hasAnyAuthority('system:admin')")
    public Result<String> add(@RequestBody Admin admin){
        //新增管理员
        return adminService.add(admin);
    }

    @DeleteMapping
    @Operation(summary = "删除管理员")
    @PreAuthorize("hasAnyAuthority('system:admin')")
    public Result<String> delete(Long id){
        adminService.removeById(id);
        return Result.success();
    }

}
