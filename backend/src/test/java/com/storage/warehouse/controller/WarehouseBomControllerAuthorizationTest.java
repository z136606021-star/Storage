package com.storage.warehouse.controller;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseBomControllerAuthorizationTest {

    @Test
    void purge_requiresAdminRoleAndDedicatedDeleteRoute() throws NoSuchMethodException {
        Method purge = WarehouseBomController.class.getMethod("purge", Long.class);

        RequiresRoles roles = purge.getAnnotation(RequiresRoles.class);
        DeleteMapping mapping = purge.getAnnotation(DeleteMapping.class);

        assertThat(roles).isNotNull();
        assertThat(roles.value()).containsExactly("ADMIN");
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/{id}/purge");
    }
}
