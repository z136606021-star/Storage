package com.storage.warehouse.mapper;

import com.storage.warehouse.dto.SafetyStockQueryDTO;
import com.storage.warehouse.service.SafetyStockWarningStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

public class SafetyStockSqlProvider {

    public String selectJoinedPage(Map<String, Object> params) {
        return buildSelectSql(params, true);
    }

    public String selectJoinedList(Map<String, Object> params) {
        return buildSelectSql(params, false);
    }

    private String buildSelectSql(Map<String, Object> params, boolean paginated) {
        SafetyStockQueryDTO query = (SafetyStockQueryDTO) params.get("query");
        StringBuilder sql = new StringBuilder();
        sql.append("""
                SELECT
                    ss.id AS safety_stock_id,
                    ml.id AS material_ledger_id,
                    ml.category,
                    ml.generic_name,
                    ml.brand,
                    ml.name,
                    ml.model,
                    ml.bin_location,
                    ml.stock_quantity,
                    COALESCE(ss.safety_quantity, 0) AS safety_quantity,
                    COALESCE(ss.warning_enabled, 0) AS warning_enabled,
                    ss.created_at,
                    ss.updated_at
                FROM material_ledger ml
                LEFT JOIN safety_stock ss ON ss.material_ledger_id = ml.id
                WHERE 1 = 1
                """);

        appendFilters(sql, query);
        sql.append(" ORDER BY CASE WHEN ")
                .append(SafetyStockWarningStatus.WARNING_SQL_EXPRESSION.trim())
                .append(" THEN 0 ELSE 1 END, ml.id ASC");

        if (paginated) {
            return sql.toString();
        }
        return sql.toString();
    }

    private void appendFilters(StringBuilder sql, SafetyStockQueryDTO query) {
        if (query == null) {
            return;
        }

        if (!CollectionUtils.isEmpty(query.getIds())) {
            sql.append(" AND ml.id IN (");
            for (int i = 0; i < query.getIds().size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("#{query.ids[").append(i).append("]}");
            }
            sql.append(") ");
        }

        if (isFilterValue(query.getCategory())) {
            sql.append(" AND ml.category = #{query.category} ");
        }
        if (isFilterValue(query.getGenericName())) {
            sql.append(" AND ml.generic_name = #{query.genericName} ");
        }
        if (isFilterValue(query.getBrand())) {
            sql.append(" AND ml.brand = #{query.brand} ");
        }
        if (StringUtils.hasText(query.getName())) {
            sql.append(" AND ml.name LIKE CONCAT('%', #{query.name}, '%') ");
        }
        if (isFilterValue(query.getModel())) {
            sql.append(" AND ml.model = #{query.model} ");
        }
        if (isFilterValue(query.getBinLocation())) {
            sql.append(" AND ml.bin_location = #{query.binLocation} ");
        }

        if (StringUtils.hasText(query.getSafetyQuantityKeyword())) {
            sql.append("""
                     AND CAST(COALESCE(ss.safety_quantity, 0) AS VARCHAR) LIKE CONCAT('%', #{query.safetyQuantityKeyword}, '%')
                    """);
        }

        if (StringUtils.hasText(query.getWarningPeriod())) {
            String warningExpr = SafetyStockWarningStatus.WARNING_SQL_EXPRESSION.trim();
            String period = query.getWarningPeriod().trim();
            if ("是".equals(period) || "YES".equalsIgnoreCase(period)) {
                sql.append(" AND ").append(warningExpr);
            } else if ("否".equals(period) || "NO".equalsIgnoreCase(period)) {
                sql.append(" AND NOT ").append(warningExpr);
            } else {
                throw new IllegalArgumentException("预警状态无效，仅支持 是、否");
            }
        }
    }

    private boolean isFilterValue(String value) {
        return StringUtils.hasText(value);
    }
}
