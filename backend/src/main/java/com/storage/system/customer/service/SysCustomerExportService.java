package com.storage.system.customer.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.system.customer.entity.SysCustomer;
import com.storage.system.customer.excel.SysCustomerExportRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public interface SysCustomerExportService {
    byte[] export(List<SysCustomer> records) throws IOException;
    byte[] exportTemplate() throws IOException;
}
