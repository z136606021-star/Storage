package com.storage.warehouse.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.mapper.MaterialLedgerMapper;
import com.storage.warehouse.shared.MaterialUsageQueryService;
import org.springframework.util.StringUtils;
public interface MaterialLedgerUsageQueryService extends MaterialUsageQueryService {
}
