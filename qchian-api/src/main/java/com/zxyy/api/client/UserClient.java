package com.zxyy.api.client;

import com.zxyy.api.dto.user.AdminLoginDTO;
import com.zxyy.api.vo.user.AdminLoginVo;
import com.zxyy.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", url = "${qchain.clients.user-url:}", contextId = "userClient")
public interface UserClient {
    @PostMapping("/admin/admins/login")
    Result<AdminLoginVo> login(@RequestBody AdminLoginDTO request);
}
