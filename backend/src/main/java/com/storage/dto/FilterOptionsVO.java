package com.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionsVO {

    private List<String> categories;

    private List<String> genericNames;

    private List<String> brands;

    private List<String> models;

    private List<String> binLocations;
}
