package com.storage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({
        "com.storage.system.customer.mapper",
        "com.storage.system.role.mapper",
        "com.storage.system.user.mapper",
        "com.storage.system.menu.mapper",
        "com.storage.system.auth.mapper",
        "com.storage.system.exceptionlog.mapper",
        "com.storage.design.mapper",
        "com.storage.experience.mapper",
        "com.storage.warehouse.mapper",
        "com.storage.infrastructure.file.mapper"
})
public class StorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageApplication.class, args);
    }
}
