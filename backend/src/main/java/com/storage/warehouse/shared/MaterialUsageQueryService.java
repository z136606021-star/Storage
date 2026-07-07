package com.storage.warehouse.shared;

public interface MaterialUsageQueryService {

    long countByBinCode(String binCode);

    long countByBomCatalogKey(String category, String genericName, String brand, String name);
}
