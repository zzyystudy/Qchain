package com.zxyy.user.exception;

import com.zxyy.common.exception.BaseException;

public class AccountLocked extends BaseException {
    public AccountLocked(String message) {
        super(message);
    }
}
