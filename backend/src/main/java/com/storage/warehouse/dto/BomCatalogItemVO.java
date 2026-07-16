package com.storage.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BomCatalogItemVO {

    private Long id;

    private String category;

    private String genericName;

    private String brand;

    private String name;

    private String displayLabel;
}
