package com.zhangfuxing.tools.gateway.ext;

import io.netty.handler.codec.http.FullHttpRequest;

/**
 * @author 张福兴
 * @version 1.0
 * @date 2025/8/4
 * @email zhangfuxing1010@163.com
 */
public class RequestInfo {
	private String method;
	private String protocol;
	private String host;
	private String uri;

	public RequestInfo(FullHttpRequest request) {
		this.method = request.method().name();
		this.protocol = request.protocolVersion().protocolName().toLowerCase();
		this.host = request.headers().get("host");
		this.uri = request.uri();
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
}
