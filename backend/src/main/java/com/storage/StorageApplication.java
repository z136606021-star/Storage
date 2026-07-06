package com.storage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "com.storage.system.customer.mapper",
        "com.storage.system.role.mapper",
        "com.storage.system.user.mapper",
        "com.storage.system.menu.mapper",
        "com.storage.system.auth.mapper",
        "com.storage.warehouse.bin.mapper",
        "com.storage.warehouse.bom.mapper",
        "com.storage.warehouse.ledger.mapper",
        "com.storage.warehouse.safety.mapper",
        "com.storage.warehouse.stats.mapper",
        "com.storage.warehouse.io.mapper",
        "com.storage.infrastructure.file.mapper"
})
public class StorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageApplication.class, args);
    }
}
