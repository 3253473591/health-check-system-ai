package com.lqx.health_check_system.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt",    LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "registerTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {}
}