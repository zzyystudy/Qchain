package com.zxyy.user.exception;

import com.zxyy.common.exception.BaseException;

public class AccountNotFound extends BaseException {
    public AccountNotFound(String message) {
        super(message);
    }
}
