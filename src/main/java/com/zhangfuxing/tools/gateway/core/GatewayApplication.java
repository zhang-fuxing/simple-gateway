package com.zhangfuxing.tools.gateway.core;

import com.zhangfuxing.tools.gateway.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 网关应用启动类
 *
 * @author 张福兴
 * @version 1.0
 * @date 2025/7/29
 * @email zhangfuxing1010@163.com
 */
public class GatewayApplication {

	private static final List<String> DEFAULT_CONFIG_FILE_NAMES =
			List.of("./conf/proxy.json", "./proxy.json");

	public static void run(int listenPort, RouteConfig... config) {
		DynamicGateway gateway = new DynamicGateway(new DynamicConfigManager(listenPort, config));
		try {
			gateway.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void run(String[] args) {
		String path = null;
		for (String arg : args) {
			if (arg.startsWith("-f=")) {
				path = arg.substring(3);
			}
		}
		GatewayConfig config = JsonUtil.toBean(loadConfig(path), GatewayConfig.class);
		var gateways = config.getHttp().stream()
				.map(c -> {
					DynamicConfigManager manager = new DynamicConfigManager(c.getPort());
					c.getRoutes().forEach(manager::addRoute);
					return manager;
				})
				.toArray(DynamicConfigManager[]::new);

		try {
			new DynamicGateway(gateways).start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String loadConfig(String path) {
		try {
			if (path != null) {
				Path configPath = Paths.get(path);
				if (!Files.exists(configPath)) {
					throw new IllegalArgumentException("系统找不到指定的配置文件：" + path);
				}
				return Files.readString(configPath);
			}

			for (String configFile : DEFAULT_CONFIG_FILE_NAMES) {
				File file = new File(configFile);
				if (file.exists()) {
					return Files.readString(file.toPath(), StandardCharsets.UTF_8);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("文件读取失败", e);
		}
		throw new RuntimeException("未找到配置文件: 请在当前目录下创建proxy.json文件并进行配置");
	}
}
