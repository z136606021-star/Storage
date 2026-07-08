package com.storage.warehouse.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.common.query.PageSupport;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
import com.storage.warehouse.converter.MaterialIoConverter;
import com.storage.warehouse.dto.MaterialIoQueryDTO;
import com.storage.warehouse.dto.MaterialIoRecordVO;
import com.storage.warehouse.dto.MaterialIoSafetyHintVO;
import com.storage.warehouse.entity.MaterialIoRecord;
import com.storage.warehouse.exception.MaterialIoNotFoundException;
import com.storage.warehouse.mapper.MaterialIoRecordMapper;
import com.storage.warehouse.query.MaterialIoQueryBuilder;
import com.storage.warehouse.dto.MaterialQueryDTO;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
import com.storage.warehouse.query.MaterialLedgerQueryBuilder;
import com.storage.warehouse.service.MaterialLedgerService;
import com.storage.warehouse.entity.SafetyStock;
import com.storage.warehouse.mapper.SafetyStockMapper;
import com.storage.warehouse.dto.FilterLinkageQueryDTO;
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
