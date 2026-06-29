package com.storage.converter;

import com.storage.dto.SysCustomerSaveDTO;
import com.storage.entity.SysCustomer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SysCustomerConverter {

    public SysCustomer toNewEntity(SysCustomerSaveDTO dto) {
        SysCustomer entity = new SysCustomer();
        applySaveDto(entity, dto);
        return entity;
    }

    public void applySaveDto(SysCustomer entity, SysCustomerSaveDTO dto) {
        entity.setCustomerCode(dto.getCustomerCode().trim());
        entity.setName(dto.getName().trim());
        entity.setContactName(trimToNull(dto.getContactName()));
        entity.setPhone(trimToNull(dto.getPhone()));
        entity.setEmail(trimToNull(dto.getEmail()));
        entity.setAddress(trimToNull(dto.getAddress()));
        entity.setRemark(trimToNull(dto.getRemark()));
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
