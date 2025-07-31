package com.zhangfuxing.tools.gateway.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author 张福兴
 * @version 1.0
 * @date 2025/7/28
 * @email zhangfuxing1010@163.com
 */
public class DynamicConfigManager {
	private static final Logger logger = LoggerFactory.getLogger(DynamicConfigManager.class);
	private final int bindPort;
	private final List<RouteConfig> routeCache = new ArrayList<>();

	public DynamicConfigManager(int bindPort) {
		this.bindPort = bindPort;
	}

	public DynamicConfigManager(int bindPort, RouteConfig... routeConfigs) {
		this.bindPort = bindPort;
		routeCache.addAll(Arrays.asList(routeConfigs));
		sortRoutes();
	}

	public void addRoute(RouteConfig routeConfig) {
		routeCache.add(routeConfig);
		sortRoutes();
	}

	public RouteConfig getRouteForPath(String path) {
		for (RouteConfig config : routeCache) {
			if (config.accept(path)) {
				return config;
			}
		}
		return null;
	}

	public int getBindPort() {
		return bindPort;
	}

	private void sortRoutes() {
		routeCache.sort(Comparator.comparingInt(RouteConfig::getOrder));
	}
}
