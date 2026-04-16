package com.ring.cloud.core.util;

import java.io.File;

public class FileCoreUtil {
    // 临时文件重命名为正式文件
    public static void renameToFinal(File tmpFile, String inputDomain, File exportDir) {
        if (tmpFile == null || !tmpFile.exists()) {
            throw new RuntimeException("临时文件不存在，重命名失败");
        }

        File finalFile = new File(exportDir, inputDomain + ".csv");

        // 删除旧文件
        if (finalFile.exists()) {
            if (!finalFile.delete()) {
                throw new RuntimeException("旧正式文件删除失败：" + finalFile);
            }
        }

        // 👇 关键：重命名失败必须抛出异常，不能静默失败
        if (!tmpFile.renameTo(finalFile)) {
            throw new RuntimeException("临时文件重命名失败：" + tmpFile + " -> " + finalFile);
        }
    }
}
