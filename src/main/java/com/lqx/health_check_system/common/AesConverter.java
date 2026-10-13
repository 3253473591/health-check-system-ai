package com.lqx.health_check_system.common;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;

public class AesConverter extends AbstractJsonTypeHandler<String> {

    @Override
    protected String parse(String json) {
        return CryptoUtils.decrypt(json);
    }

    @Override
    protected String toJson(String obj) {
        return CryptoUtils.encrypt(obj);
    }
}