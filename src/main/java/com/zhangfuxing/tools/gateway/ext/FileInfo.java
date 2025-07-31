package com.zhangfuxing.tools.gateway.ext;

import com.zhangfuxing.tools.gateway.util.DataSizeFormat;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * @author 张福兴
 * @version 1.0
 * @date 2025/7/31
 * @email zhangfuxing1010@163.com
 */
public class FileInfo {
	public String uri;
	public String path;
	public String name;
	public String type;
	public String size;
	private int dir;
	private int canView;

	public FileInfo() {
	}

	public FileInfo(File file, Path rootPath) {
		String string = rootPath.relativize(file.toPath()).toString();
		string = string.replaceAll("\\\\", "/");
		this.uri = URLEncoder.encode(string, StandardCharsets.UTF_8);
		this.path = file.getAbsolutePath();
		this.name = file.getName();
		this.type = file.isDirectory() ? "目录" : "文件";
		this.size = DataSizeFormat.formatSize(file.length());
		this.dir = file.isDirectory() ? 1 : 0;
		this.canView = file.isFile() && !getContentType(this.path).equals("application/octet-stream") ? 1 : 0;
	}

	public static String getContentType(String filePath, String... query) {
		for (String p : query) {
			if (p == null) {
				continue;
			}
			if (p.contains("action=download")) {
				return "application/octet-stream";
			}
		}

		filePath = filePath.toLowerCase();
		if (filePath.endsWith(".html")) {
			return "text/html; charset=UTF-8";
		} else if (filePath.endsWith(".css")) {
			return "text/css; charset=UTF-8";
		} else if (filePath.endsWith(".js")) {
			return "application/javascript; charset=UTF-8";
		} else if (filePath.endsWith(".json")) {
			return "application/json; charset=UTF-8";
		} else if (filePath.endsWith(".png")) {
			return "image/png";
		} else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
			return "image/jpeg";
		} else if (filePath.endsWith(".gif")) {
			return "image/gif";
		} else if (filePath.endsWith(".svg")) {
			return "image/svg+xml";
		} else if (filePath.endsWith(".ico")) {
			return "image/x-icon";
		} else if (filePath.endsWith(".webp")) {
			return "image/webp";
		} else {
			return "application/octet-stream";
		}
	}

	public int getCanView() {
		return canView;
	}

	public FileInfo setCanView(int canView) {
		this.canView = canView;
		return this;
	}

	public int getDir() {
		return dir;
	}

	public FileInfo setDir(int dir) {
		this.dir = dir;
		return this;
	}

	public String getUri() {
		return uri;
	}

	public FileInfo setUri(String uri) {
		this.uri = uri;
		return this;
	}

	public String getPath() {
		return path;
	}

	public FileInfo setPath(String path) {
		this.path = path;
		return this;
	}

	public String getName() {
		return name;
	}

	public FileInfo setName(String name) {
		this.name = name;
		return this;
	}

	public String getType() {
		return type;
	}

	public FileInfo setType(String type) {
		this.type = type;
		return this;
	}

	public String getSize() {
		return size;
	}

	public FileInfo setSize(String size) {
		this.size = size;
		return this;
	}
}
