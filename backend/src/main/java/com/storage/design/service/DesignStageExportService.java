package com.storage.design.service;

import com.storage.design.entity.DesignStage;

import java.io.IOException;
import java.util.List;

public interface DesignStageExportService {

    byte[] export(List<DesignStage> records) throws IOException;
}
