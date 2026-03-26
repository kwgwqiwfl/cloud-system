package com.ring.cloud.facade.util;

import java.io.*;
import java.nio.file.Files;

public class FileUtil {
	public static final int BATCH_SIZE = 1024 * 32;    // 32KB 一批
	public static final int BUFFER_SIZE = 1024 * 1024; // 1MB 写缓冲
	public static final String fileNameConnector = "_"; //文件名连接符

	//生成ip csv文件地址 路径加名称
	public static String ipCsvFileName(String path, String ipFileNamePrefix, int seg) {
		// 2. 跨平台路径拼接（自动适配Windows/Linux/Mac）
		File baseDir = new File(path);
		File csvFile = new File(baseDir, ipFileNamePrefix + fileNameConnector + seg + ".csv");

		// 3. 返回标准化路径（自动处理分隔符、冗余斜杠等）
		return csvFile.getAbsolutePath();
	}

	public static void forceCreateFile(String filePath) throws IOException {
		File file = new File(filePath);

		// 1. 先创建父目录（跨平台）
		File parentDir = file.getParentFile();
		if (!parentDir.exists()) {
			if (!parentDir.mkdirs()) {
				throw new IOException("创建目录失败：" + parentDir.getAbsolutePath());
			}
		}

		// 2. 如果文件已存在，先删除（解决文件被锁定/占用）
		if (file.exists()) {
			if (!file.delete()) {
				throw new IOException("旧文件无法删除，可能被占用：" + filePath);
			}
		}

		// 3. 新建空白文件
		if (!file.createNewFile()) {
			throw new IOException("文件创建失败：" + filePath);
		}
	}

	public static void renameTmpToCsv(String tmpPath, String csvPath) throws IOException {
		File tmpFile = new File(tmpPath);
		File csvFile = new File(csvPath);

		// 👇 关键：已存在就删除
		if (csvFile.exists()) {
			if (!csvFile.delete()) {
				throw new IOException("无法删除旧csv文件：" + csvPath);
			}
		}

		// 再执行改名
		Files.move(tmpFile.toPath(), csvFile.toPath());
	}


	public static void write(String content) {
		File file = null;
		PrintWriter pw = null;
		BufferedWriter bw = null;
		try {
			String fileName = "D://crawl/course.txt";
			file = new File(fileName);
			if (!file.exists())
				file.createNewFile();
			else{
				file.delete();
				file = new File(fileName);
				if (!file.exists())
					file.createNewFile();
			}
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "GBK"));
			bw = new BufferedWriter(pw);
			bw.write(content);
			bw.flush();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (pw != null)
					pw.close();
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String read(String path) {
		BufferedReader reader = null;
		try {
			StringBuilder str = new StringBuilder();
			FileInputStream fileInputStream = new FileInputStream(path);
			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "GBK");
			reader = new BufferedReader(inputStreamReader);
			String line;
			while ((line = reader.readLine()) != null) {
				str.append(line);
			}
			return str.toString();
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage());
		}finally {
			try {
				if(reader!=null)
					reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		String tmpFilePath = "D:\\crawl\\xml\\ip_1.csv.tmp";
		String formalFilePath = "D:\\crawl\\xml\\ip_1.csv";
		File tmpFile = new File(tmpFilePath);
		File formalFile = new File(formalFilePath);

		// 校验tmp文件存在且非空（避免转正空文件）
		if (!tmpFile.exists() || tmpFile.length() == 0) {
			throw new IOException("tmp文件为空或不存在，拒绝转正：" + tmpFilePath);
		}

		// 原子操作：先删旧正式文件，再重命名tmp（文件系统级原子性）
		if (formalFile.exists() && !formalFile.delete()) {
			throw new IOException("删除旧正式文件失败：" + formalFilePath);
		}
		if (!tmpFile.renameTo(formalFile)) {
			throw new IOException("tmp文件转正失败：" + tmpFilePath + " → " + formalFilePath);
		}
	}
}
