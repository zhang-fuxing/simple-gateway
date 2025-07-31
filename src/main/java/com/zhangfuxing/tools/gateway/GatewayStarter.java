package com.zhangfuxing.tools.gateway;

import com.zhangfuxing.tools.gateway.core.GatewayApplication;
import com.zhangfuxing.tools.gateway.core.GatewayConfig;
import com.zhangfuxing.tools.gateway.core.RouteConfig;
import com.zhangfuxing.tools.gateway.util.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 张福兴
 * @version 1.0
 * @date 2025/7/29
 * @email zhangfuxing1010@163.com
 */
public class GatewayStarter {
	public static void main(String[] args) throws Exception {
		for (String arg : args) {
			if (arg.startsWith("--config-create")) {
				String path = null;
				if (arg.contains("=")) {
					path = arg.split("=")[1];
				}
				generateConfig(path);
				return;
			}
			if (arg.equals("--help")) {
				System.out.println("Usage:");
				System.out.println("\t系统默认使用当前目录下的proxy.json或者./conf/proxy.json作为配置文件，如果不存在请指定配置文件");
				System.out.println("\t-f=<path>: 指定配置文件路径,需要的内容为JSON, 可使用 --config-create=/path/config.json 选项生成配置文件");
				System.out.println("\t--config-create=[<path>]: 创建配置文件, 路径如果不指定则在当前目录下生成文件, 可使用 -f=/path/config.json 加载配置文件");
				System.out.println("\t--help: 显示帮助信息");
				return;
			}
		}
		GatewayApplication.run(args);
	}

	public static void generateConfig(String path) {
		GatewayConfig gatewayConfig = new GatewayConfig();
		List<GatewayConfig.HttpConfig> http = new ArrayList<>();
		gatewayConfig.setHttp(http);
		GatewayConfig.HttpConfig config = new GatewayConfig.HttpConfig();
		config.setPort(80);
		List<RouteConfig> routes = new ArrayList<>();
		config.setRoutes(routes);
		routes.add(new RouteConfig()
				.setSource("/")
				.setRoot("/opt/www/web-resource/app1")
				.setIndex("index.html")
				.setOrder(100)
				.setDescription("前端资源配置"));
		routes.add(new RouteConfig()
				.setSource("^/api/(.*)")
				.setTarget("http://localhost:8080/api/$1")
				.setOrder(1)
				.setDescription("API接口代理，将api开头的uri代理到target，target中的$1表示(.*)匹配的部分，即/api/之后的所有内容"));
		http.add(config);
		if (path == null || path.isBlank()) {
			path = "./proxy.json";
		}
		Path outpath = Paths.get(path);
		try {
			if (!Files.exists(outpath.getParent())) {
				Files.createDirectories(outpath.getParent());
			}
			Files.writeString(outpath, JsonUtil.format(gatewayConfig));
			System.out.println("配置文件生成成功：" + outpath);
		} catch (IOException e) {
			System.out.println("配置文件生成失败：" + e.getMessage());
		}
	}
}
