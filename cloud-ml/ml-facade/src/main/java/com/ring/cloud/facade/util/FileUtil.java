package com.ring.cloud.facade.util;

import com.ring.cloud.facade.entity.ip.TaskEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileUtil {
	public static final int BATCH_SIZE = 1024 * 32;    // 32KB 一批
	public static final int BUFFER_SIZE = 1024 * 1024; // 1MB 写缓冲
	public static final String fileNameConnector = "_"; //文件名连接符

	//生成ip csv文件地址 路径加名称
	public static String ipCsvFileName(String path, String ipFileNamePrefix, String seg) {
		// 2. 跨平台路径拼接（自动适配Windows/Linux/Mac）
		File baseDir = new File(path);
		File csvFile = new File(baseDir, ipFileNamePrefix + fileNameConnector + seg + ".csv");

		// 3. 返回标准化路径（自动处理分隔符、冗余斜杠等）
		return csvFile.getAbsolutePath();
	}
	//生成ip csv文件地址 路径加名称
	public static String ipCsvFileName(String path, String fileName) {
		// 2. 跨平台路径拼接（自动适配Windows/Linux/Mac）
		File baseDir = new File(path);
		File csvFile = new File(baseDir, fileName);

		// 3. 返回标准化路径（自动处理分隔符、冗余斜杠等）
		return csvFile.getAbsolutePath();
	}

	public static void forceCreateFile(String filePath) throws IOException {
		File file = new File(filePath);

		// ====================== 完全注释掉目录创建 ======================
		// 提前建好目录，不需要这一段！
	/*
    File parentDir = file.getParentFile();
    if (!parentDir.exists()) {
        if (!parentDir.mkdirs()) {
            throw new IOException("创建目录失败：" + parentDir.getAbsolutePath());
        }
    }
	*/

		// 2. 如果文件已存在，先删除
		if (file.exists()) {
			if (!file.delete()) {
				throw new IOException("旧文件无法删除，可能被占用：" + filePath);
			}
		}

		// 3. 只创建文件，不碰目录
		if (!file.createNewFile()) {
			throw new IOException("文件创建失败：" + filePath);
		}
	}

	//文件改名
	public static void renameTmpToFile(String tmpPath, String filePath) throws IOException {
		File tmpFile = new File(tmpPath);
		File csvFile = new File(filePath);

		// 👇 关键：已存在就删除
		if (csvFile.exists()) {
			if (!csvFile.delete()) {
				throw new IOException("无法删除旧csv文件：" + filePath);
			}
		}

		// 再执行改名
		Files.move(tmpFile.toPath(), csvFile.toPath());
	}
	/**
	 * 异常时删除临时文件
	 */
	public static void deleteTmpFile(String tmpPath) {
		try {
			File tmpFile = new File(tmpPath);
			if (tmpFile.exists()) {
				tmpFile.delete();
			}
		} catch (Exception ignored) {}
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

	/**
	 * 通用导入文件读取工具：读取文本文件 → 去重 → 小写 → 校验行数 → 返回 List
	 */
	public static List<String> readFileToList(MultipartFile file) {
		// 文件不能为空
		if (file == null || file.isEmpty()) {
			throw new RuntimeException("上传文件不能为空");
		}

		Set<String> dataSet = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

			String line;
			while ((line = reader.readLine()) != null) {
				String data = line.trim().toLowerCase();
				if (!data.isEmpty()) {
					dataSet.add(data);

					// 最大 200 万行限制
					if (dataSet.size() > 2000000) {
						throw new RuntimeException("文件有效行数超出限制，最大允许导入 200 万行");
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("导入文件读取失败：" + e.getMessage(), e);
		}

		List<String> dataList = new ArrayList<>(dataSet);
		if (dataList.isEmpty()) {
			throw new RuntimeException("文件中无有效数据");
		}

		return dataList;
	}

	public static void mergeAllSubdomainFiles(TaskEntity task) {
		String timeStamp = task.getTimeStamp();
		String outPath = task.getOutPath();
		File outputDir = new File(outPath);

		File finalCsv = new File(outputDir, "子域名_" + timeStamp + ".csv");
		String suffix = "_" + timeStamp + ".tmp";

		File[] tmpFiles = outputDir.listFiles((dir, name) -> name.endsWith(suffix));
		if (tmpFiles == null || tmpFiles.length == 0) {
			return;
		}

		byte[] buffer = new byte[8192]; // 8K 标准快读缓冲区

		try (OutputStream out = Files.newOutputStream(finalCsv.toPath())) {
			for (File tmp : tmpFiles) {
				try (InputStream in = Files.newInputStream(tmp.toPath())) {
					int len;
					while ((len = in.read(buffer)) != -1) {
						out.write(buffer, 0, len);
					}
				}
				Files.deleteIfExists(tmp.toPath());
			}
		} catch (Exception e) {
			// 不抛出异常
		}
	}
}
