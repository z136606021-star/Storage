package com.storage.design.service;

import com.storage.design.entity.DesignGuide;

import java.io.IOException;
import java.util.List;

public interface DesignGuideExportService {

    byte[] export(List<DesignGuide> records) throws IOException;

    byte[] exportTemplate() throws IOException;
}
