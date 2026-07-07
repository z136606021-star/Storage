package com.storage.warehouse.io.service;

import com.storage.warehouse.io.mapper.MaterialIoRecordMapper;
import com.storage.warehouse.shared.MaterialIoUsageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MaterialIoUsageQueryServiceImpl implements MaterialIoUsageQueryService {

    private final MaterialIoRecordMapper materialIoRecordMapper;

    @Override
    public long countByMaterialLedgerId(Long materialLedgerId) {
        return materialIoRecordMapper.countByMaterialLedgerId(materialLedgerId);
    }
}
