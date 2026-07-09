package com.storage.design.service;

import com.storage.design.entity.DesignProductType;

import java.io.IOException;
import java.util.List;

public interface DesignProductTypeExportService {

    byte[] export(List<DesignProductType> records) throws IOException;
}
