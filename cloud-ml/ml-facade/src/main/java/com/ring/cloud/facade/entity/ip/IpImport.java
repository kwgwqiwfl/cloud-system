package com.ring.cloud.facade.entity.ip;

import lombok.Data;

/**
 * 导入表
 */
@Data
public class IpImport {
    private String tableName;
    private String filePath;
    private String fileName;

}
