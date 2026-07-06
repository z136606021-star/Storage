package com.storage.system.customer.service;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.system.customer.dto.SysCustomerQueryDTO;
import com.storage.system.customer.dto.SysCustomerSaveDTO;
import com.storage.system.customer.entity.SysCustomer;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

public interface SysCustomerService {

    PageResult<SysCustomer> page(SysCustomerQueryDTO query);

    SysCustomer getById(Long id);

    SysCustomer create(SysCustomerSaveDTO dto);

    SysCustomer update(Long id, SysCustomerSaveDTO dto);

    void delete(Long id);

    void batchDelete(BatchDeleteDTO dto);

    List<SysCustomer> listByQuery(SysCustomerQueryDTO query);

    byte[] export(SysCustomerQueryDTO query) throws IOException;

    byte[] exportTemplate() throws IOException;

    static String formatStatusLabel(Integer status) {
        return status != null && status == 1 ? "启用" : "停用";
    }

    static Integer parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return 1;
        }
        String trimmed = value.trim();
        if ("启用".equals(trimmed) || "1".equals(trimmed)) {
            return 1;
        }
        if ("停用".equals(trimmed) || "0".equals(trimmed)) {
            return 0;
        }
        throw new IllegalArgumentException("状态无效，请填写「启用」或「停用」");
    }
}
