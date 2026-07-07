package com.storage.warehouse.io.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.common.query.PageSupport;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
import com.storage.warehouse.io.converter.MaterialIoConverter;
import com.storage.warehouse.io.dto.MaterialIoQueryDTO;
import com.storage.warehouse.io.dto.MaterialIoRecordVO;
import com.storage.warehouse.io.dto.MaterialIoSafetyHintVO;
import com.storage.warehouse.io.entity.MaterialIoRecord;
import com.storage.warehouse.io.exception.MaterialIoNotFoundException;
import com.storage.warehouse.io.mapper.MaterialIoRecordMapper;
import com.storage.warehouse.io.query.MaterialIoQueryBuilder;
import com.storage.warehouse.ledger.dto.MaterialQueryDTO;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.mapper.MaterialLedgerMapper;
import com.storage.warehouse.ledger.query.MaterialLedgerQueryBuilder;
import com.storage.warehouse.ledger.service.MaterialLedgerService;
import com.storage.warehouse.safety.entity.SafetyStock;
import com.storage.warehouse.safety.mapper.SafetyStockMapper;
import com.storage.warehouse.shared.dto.FilterLinkageQueryDTO;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
public interface MaterialIoReadService {
    PageResult<MaterialIoRecordVO> page(MaterialIoQueryDTO query);
    MaterialIoRecordVO getById(Long id);
    List<MaterialIoRecordVO> listByQuery(MaterialIoQueryDTO query);
    FilterOptionsVO filterOptions(FilterLinkageQueryDTO query);
    List<MaterialIoSafetyHintVO> safetyHints(List<Long> materialLedgerIds);
}
