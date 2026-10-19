package com.zxyy.user.handler;

import com.zxyy.common.constant.MessageConstant;
import com.zxyy.common.exception.BaseException;
import com.zxyy.common.result.Result;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBusinessException(BaseException exception) {
        return Result.error(exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException exception) {
        String message = exception instanceof DisabledException
                ? MessageConstant.ACCOUNT_LOCKED : "用户名或密码错误";
        return new Result<>(401, message, null);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException exception) {
        return Result.error("记录已存在");
    }
}
