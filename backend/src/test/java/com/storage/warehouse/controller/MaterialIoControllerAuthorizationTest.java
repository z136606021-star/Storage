package com.storage.warehouse.controller;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialIoControllerAuthorizationTest {

    @Test
    void writeEndpointsUseGranularPermissions() throws NoSuchMethodException {
        assertPermission("batchCreate", "warehouse:material-io:create", com.storage.warehouse.dto.MaterialIoBatchSaveDTO.class);
        assertPermission("update", "warehouse:material-io:update", Long.class, com.storage.warehouse.dto.MaterialIoUpdateDTO.class);
        assertPermission("delete", "warehouse:material-io:delete", Long.class);
        assertPermission("batchDelete", "warehouse:material-io:delete", com.storage.common.dto.BatchDeleteDTO.class);
        assertPermission("deleteAll", "warehouse:material-io:delete-all");
        assertPermission("importTemplate", "warehouse:material-io:import");
        assertPermission("importExcel", "warehouse:material-io:import", org.springframework.web.multipart.MultipartFile.class);
        assertPermission("export", "warehouse:material-io:export", com.storage.warehouse.dto.MaterialIoQueryDTO.class);
    }

    private void assertPermission(String methodName, String expected, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = MaterialIoController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(RequiresPermissions.class).value()).containsExactly(expected);
    }
}
