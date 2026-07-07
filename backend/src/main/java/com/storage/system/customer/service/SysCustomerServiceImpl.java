package com.storage.system.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.common.exception.BusinessException;
import com.storage.common.query.PageSupport;
import com.storage.system.customer.converter.SysCustomerConverter;
import com.storage.system.customer.dto.SysCustomerQueryDTO;
import com.storage.system.customer.dto.SysCustomerSaveDTO;
import com.storage.system.customer.entity.SysCustomer;
import com.storage.system.customer.exception.SysCustomerNotFoundException;
import com.storage.system.customer.mapper.SysCustomerMapper;
import com.storage.system.customer.query.SysCustomerQueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysCustomerServiceImpl implements SysCustomerService {

    private final SysCustomerMapper sysCustomerMapper;
    private final SysCustomerConverter sysCustomerConverter;
    private final SysCustomerExportService sysCustomerExportService;

    @Override
    public PageResult<SysCustomer> page(SysCustomerQueryDTO query) {
        var result = sysCustomerMapper.selectPage(
                PageSupport.page(query.getPage(), query.getPageSize()),
                SysCustomerQueryBuilder.build(query)
        );
        return PageSupport.result(result);
    }

    @Override
    public SysCustomer getById(Long id) {
        SysCustomer customer = sysCustomerMapper.selectById(id);
        if (customer == null) {
            throw new SysCustomerNotFoundException(id);
        }
        return customer;
    }

    @Override
    public SysCustomer create(SysCustomerSaveDTO dto) {
        normalizeSaveDto(dto);
        assertCustomerCodeUnique(dto.getCustomerCode(), null);
        SysCustomer entity = sysCustomerConverter.toNewEntity(dto);
        sysCustomerMapper.insert(entity);
        return entity;
    }

    @Override
    public SysCustomer update(Long id, SysCustomerSaveDTO dto) {
        SysCustomer existing = getById(id);
        normalizeSaveDto(dto);
        assertCustomerCodeUnique(dto.getCustomerCode(), id);
        sysCustomerConverter.applySaveDto(existing, dto);
        sysCustomerMapper.updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        sysCustomerMapper.deleteById(id);
    }

    @Override
    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            delete(id);
        }
    }

    @Override
    public List<SysCustomer> listByQuery(SysCustomerQueryDTO query) {
        return sysCustomerMapper.selectList(SysCustomerQueryBuilder.build(query));
    }

    @Override
    public byte[] export(SysCustomerQueryDTO query) throws IOException {
        return sysCustomerExportService.export(listByQuery(query));
    }

    @Override
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
}
