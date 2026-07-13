package com.storage.system.customer.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.customer.dto.SysCustomerQueryDTO;
import com.storage.system.customer.dto.SysCustomerSaveDTO;
import com.storage.system.customer.entity.SysCustomer;
import com.storage.system.customer.mapper.SysCustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SysCustomerServiceIntegrationTest {

    @Autowired
    private SysCustomerService sysCustomerService;

    @Autowired
    private SysCustomerMapper sysCustomerMapper;

    @BeforeEach
    void setUp() {
        sysCustomerMapper.delete(null);
    }

    @Test
    void createAndPageCustomer() {
        SysCustomerSaveDTO dto = saveDto("CUST-100", "测试客户");
        SysCustomer created = sysCustomerService.create(dto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCustomerCode()).isEqualTo("CUST-100");
        assertThat(created.getName()).isEqualTo("测试客户");

        SysCustomerQueryDTO query = new SysCustomerQueryDTO();
        query.setName("测试");
        query.setPage(1);
        query.setPageSize(10);

        var page = sysCustomerService.page(query);
        assertThat(page.getRecords()).hasSize(1);
    }

    @Test
    void duplicateCustomerCode_rejects() {
        sysCustomerService.create(saveDto("CUST-DUP", "客户A"));

        assertThatThrownBy(() -> sysCustomerService.create(saveDto("CUST-DUP", "客户B")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("客户编号已存在");
    }

    @Test
    void updateCustomer_changesName() {
        SysCustomer created = sysCustomerService.create(saveDto("CUST-UPD", "旧名称"));

        SysCustomerSaveDTO update = saveDto("CUST-UPD", "新名称");
        SysCustomer updated = sysCustomerService.update(created.getId(), update);

        assertThat(updated.getName()).isEqualTo("新名称");
    }

    @Test
    void createCustomer_normalizesMixedCaseEmailToLowercase() {
        SysCustomerSaveDTO dto = saveDto("CUST-EMAIL", "邮箱客户");
        dto.setEmail("  Sales@Customer.COM  ");

        SysCustomer created = sysCustomerService.create(dto);

        assertThat(created.getEmail()).isEqualTo("sales@customer.com");
        assertThat(sysCustomerMapper.selectById(created.getId()).getEmail()).isEqualTo("sales@customer.com");
    }

    private SysCustomerSaveDTO saveDto(String code, String name) {
        SysCustomerSaveDTO dto = new SysCustomerSaveDTO();
        dto.setCustomerCode(code);
        dto.setName(name);
        dto.setStatus(1);
        return dto;
    }
}
