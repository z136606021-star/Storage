package com.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storage.converter.SysCustomerConverter;
import com.storage.dto.BatchDeleteDTO;
import com.storage.dto.PageResult;
import com.storage.dto.SysCustomerQueryDTO;
import com.storage.dto.SysCustomerSaveDTO;
import com.storage.entity.SysCustomer;
import com.storage.exception.BusinessException;
import com.storage.exception.SysCustomerNotFoundException;
import com.storage.mapper.SysCustomerMapper;
import com.storage.query.SysCustomerQueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysCustomerService {

    private final SysCustomerMapper sysCustomerMapper;
    private final SysCustomerConverter sysCustomerConverter;
    private final SysCustomerExportService sysCustomerExportService;

    public PageResult<SysCustomer> page(SysCustomerQueryDTO query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        Page<SysCustomer> result = sysCustomerMapper.selectPage(
                new Page<>(page, pageSize),
                SysCustomerQueryBuilder.build(query)
        );
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public SysCustomer getById(Long id) {
        SysCustomer customer = sysCustomerMapper.selectById(id);
        if (customer == null) {
            throw new SysCustomerNotFoundException(id);
        }
        return customer;
    }

    public SysCustomer create(SysCustomerSaveDTO dto) {
        normalizeSaveDto(dto);
        assertCustomerCodeUnique(dto.getCustomerCode(), null);
        SysCustomer entity = sysCustomerConverter.toNewEntity(dto);
        sysCustomerMapper.insert(entity);
        return entity;
    }

    public SysCustomer update(Long id, SysCustomerSaveDTO dto) {
        SysCustomer existing = getById(id);
        normalizeSaveDto(dto);
        assertCustomerCodeUnique(dto.getCustomerCode(), id);
        sysCustomerConverter.applySaveDto(existing, dto);
        sysCustomerMapper.updateById(existing);
        return existing;
    }

    public void delete(Long id) {
        getById(id);
        sysCustomerMapper.deleteById(id);
    }

    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            delete(id);
        }
    }

    public List<SysCustomer> listByQuery(SysCustomerQueryDTO query) {
        return sysCustomerMapper.selectList(SysCustomerQueryBuilder.build(query));
    }

    public byte[] export(SysCustomerQueryDTO query) throws IOException {
        return sysCustomerExportService.export(listByQuery(query));
    }

    public byte[] exportTemplate() throws IOException {
        return sysCustomerExportService.exportTemplate();
    }

    private void normalizeSaveDto(SysCustomerSaveDTO dto) {
        if (!StringUtils.hasText(dto.getCustomerCode())) {
            throw new BusinessException("客户编号不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException("客户名称不能为空");
        }
        dto.setCustomerCode(dto.getCustomerCode().trim());
        dto.setName(dto.getName().trim());
        if (dto.getStatus() == null) {
            dto.setStatus(1);
        }
        if (dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new BusinessException("状态无效，仅支持启用或停用");
        }
    }

    private void assertCustomerCodeUnique(String customerCode, Long excludeId) {
        LambdaQueryWrapper<SysCustomer> wrapper = Wrappers.<SysCustomer>lambdaQuery()
                .eq(SysCustomer::getCustomerCode, customerCode.trim());
        if (excludeId != null) {
            wrapper.ne(SysCustomer::getId, excludeId);
        }
        if (sysCustomerMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("客户编号已存在: " + customerCode);
        }
    }

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
