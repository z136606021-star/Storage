package com.storage.warehouse.io.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.warehouse.io.dto.MaterialIoQueryDTO;
import com.storage.warehouse.io.entity.MaterialIoRecord;
import com.storage.warehouse.ledger.query.MaterialLedgerQueryBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public final class MaterialIoQueryBuilder {

    private MaterialIoQueryBuilder() {
    }

    public static LambdaQueryWrapper<MaterialIoRecord> build(MaterialIoQueryDTO query, Collection<Long> materialLedgerIds) {
        LambdaQueryWrapper<MaterialIoRecord> wrapper = Wrappers.lambdaQuery();

        if (!CollectionUtils.isEmpty(query.getIds())) {
            wrapper.in(MaterialIoRecord::getId, query.getIds());
        }

        if (materialLedgerIds != null) {
            if (materialLedgerIds.isEmpty()) {
                wrapper.eq(MaterialIoRecord::getId, -1L);
            } else {
                wrapper.in(MaterialIoRecord::getMaterialLedgerId, materialLedgerIds);
            }
        }

        if (MaterialLedgerQueryBuilder.isFilterValue(query.getIoType())) {
            wrapper.eq(MaterialIoRecord::getIoType, normalizeIoType(query.getIoType()));
        }

        if (MaterialLedgerQueryBuilder.isFilterValue(query.getPurpose())) {
            wrapper.eq(MaterialIoRecord::getPurpose, query.getPurpose());
        }

        if (StringUtils.hasText(query.getProjectRef())) {
            wrapper.like(MaterialIoRecord::getProjectRef, query.getProjectRef().trim());
        }

        if (query.getOperatedAtStart() != null) {
            wrapper.ge(MaterialIoRecord::getOperatedAt, query.getOperatedAtStart().atStartOfDay());
        }
        if (query.getOperatedAtEnd() != null) {
            wrapper.le(MaterialIoRecord::getOperatedAt, query.getOperatedAtEnd().atTime(LocalTime.MAX));
        }

        wrapper.orderByDesc(MaterialIoRecord::getOperatedAt).orderByDesc(MaterialIoRecord::getId);
        return wrapper;
    }

    public static boolean hasMaterialFilters(MaterialIoQueryDTO query) {
        return MaterialLedgerQueryBuilder.isFilterValue(query.getCategory())
                || MaterialLedgerQueryBuilder.isFilterValue(query.getGenericName())
                || MaterialLedgerQueryBuilder.isFilterValue(query.getBrand())
                || StringUtils.hasText(query.getName())
                || MaterialLedgerQueryBuilder.isFilterValue(query.getModel())
                || MaterialLedgerQueryBuilder.isFilterValue(query.getBinLocation());
    }

    public static String normalizeIoType(String ioType) {
        if (!StringUtils.hasText(ioType)) {
            return ioType;
        }
        String value = ioType.trim();
        if ("入库".equals(value)) {
            return "IN";
        }
        if ("出库".equals(value)) {
            return "OUT";
        }
        return value.toUpperCase();
    }

    public static boolean isInbound(String ioType) {
        return "IN".equals(normalizeIoType(ioType));
    }

    public static boolean isOutbound(String ioType) {
        return "OUT".equals(normalizeIoType(ioType));
    }

    public static void assertValidIoType(String ioType) {
        if (!isInbound(ioType) && !isOutbound(ioType)) {
            throw new IllegalArgumentException("操作类型无效，仅支持入库或出库");
        }
    }

    public static String ioTypeLabel(String ioType) {
        if (isInbound(ioType)) {
            return "入库";
        }
        if (isOutbound(ioType)) {
            return "出库";
        }
        return ioType;
    }
}
