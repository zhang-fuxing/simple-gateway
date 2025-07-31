package com.zhangfuxing.tools.gateway.core;

import java.util.List;

/**
 * @author 张福兴
 * @version 1.0
 * @date 2025/7/29
 * @email zhangfuxing1010@163.com
 */
public class GatewayConfig {

	private List<HttpConfig> http;

	public GatewayConfig() {
	}

	public static class HttpConfig {
		private int port;
		private List<RouteConfig> routes;

		public int getPort() {
			return port;
		}

		public HttpConfig setPort(int port) {
			this.port = port;
			return this;
		}

		public List<RouteConfig> getRoutes() {
			return routes;
		}

		public HttpConfig setRoutes(List<RouteConfig> routes) {
			this.routes = routes;
			return this;
		}
	}

	public List<HttpConfig> getHttp() {
		return http;
	}

	public GatewayConfig setHttp(List<HttpConfig> http) {
		this.http = http;
		return this;
	}
}
