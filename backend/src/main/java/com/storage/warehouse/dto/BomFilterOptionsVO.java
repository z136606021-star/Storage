package com.storage.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BomFilterOptionsVO {

    private List<String> categories;

    private List<String> genericNames;

    private List<String> brands;
}
