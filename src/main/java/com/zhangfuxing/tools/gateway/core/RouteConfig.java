package com.zhangfuxing.tools.gateway.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 张福兴
 * @version 1.0
 * @date 2025/7/28
 * @email zhangfuxing1010@163.com
 */
public class RouteConfig {
	/**
	 * 路由ID
	 */
	private String id;
	/**
	 * 匹配路径
	 */
	private String source;
	/**
	 * 代理目标路径
	 */
	private String target;
	/**
	 * 路由匹配排序
	 */
	private int order = Integer.MAX_VALUE;
	/**
	 * WEB应用根路径
	 */
	private String root;
	/**
	 * WEB应用首页
	 */
	private String index = "index.html";
	/**
	 * 配置说明
	 */
	private String description;
	@JsonIgnore
	private Map<String, String> predicates = new HashMap<>();
	@JsonIgnore
	private Map<String, String> filters = new HashMap<>();
	@JsonIgnore
	private Map<String, String> metadata = new HashMap<>();
	private Map<String, String> responseHeaders = new HashMap<>();
	private Map<String, String> requestHeaders = new HashMap<>();
	@JsonIgnore
	private boolean isActive;
	private volatile Pattern compile;

	public RouteConfig() {
		this.id = UUID.randomUUID().toString();
		this.isActive = true;
	}

	public boolean accept(String path) {
		initPattern();
		Matcher matcher = compile.matcher(path);
		return matcher.find();
	}

	/**
	 * 是否是静态资源代理路由
	 *
	 * @return true:是静态资源代理路由 false: API路由
	 */
	public boolean resourceRoute() {
		return !(root == null || root.isBlank());
	}

	public String routingUrl(FullHttpRequest request) {
		return routingUrl(request.uri());
	}

	public String routingUrl(String uri) {
		initPattern();
		return this.compile.matcher(uri).replaceFirst(Objects.requireNonNullElse(this.target, ""));
	}

	public String getId() {
		return id;
	}

	public RouteConfig setId(String id) {
		this.id = id;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public RouteConfig setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getSource() {
		return source;
	}

	public RouteConfig setSource(String source) {
		this.source = source;
		return this;
	}

	public String getTarget() {
		return target;
	}

	public RouteConfig setTarget(String target) {
		this.target = target;
		return this;
	}

	public Map<String, String> getPredicates() {
		return predicates;
	}

	public String getRoot() {
		return root;
	}

	public RouteConfig setRoot(String root) {
		this.root = root;
		return this;
	}

	public String getIndex() {
		return index;
	}

	public RouteConfig setIndex(String index) {
		this.index = index;
		return this;
	}

	public RouteConfig setPredicates(Map<String, String> predicates) {
		this.predicates = predicates;
		return this;
	}

	public Map<String, String> getFilters() {
		return filters;
	}

	public RouteConfig setFilters(Map<String, String> filters) {
		this.filters = filters;
		return this;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public RouteConfig setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
		return this;
	}

	public boolean isActive() {
		return isActive;
	}

	public RouteConfig setActive(boolean active) {
		isActive = active;
		return this;
	}

	public int getOrder() {
		return order;
	}

	public RouteConfig setOrder(int order) {
		this.order = order;
		return this;
	}

	public Map<String, String> getResponseHeaders() {
		return responseHeaders;
	}

	public RouteConfig setResponseHeaders(Map<String, String> responseHeaders) {
		this.responseHeaders = responseHeaders;
		return this;
	}

	public Map<String, String> getRequestHeaders() {
		return requestHeaders;
	}

	public RouteConfig setRequestHeaders(Map<String, String> requestHeaders) {
		this.requestHeaders = requestHeaders;
		return this;
	}

	private void initPattern() {
		if (compile == null) {
			synchronized (RouteConfig.class) {
				if (compile == null) {
					compile = Pattern.compile(source);
				}
			}
		}
	}
}
