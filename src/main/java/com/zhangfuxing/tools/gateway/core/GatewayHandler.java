package com.zhangfuxing.tools.gateway.core;

import com.zhangfuxing.tools.gateway.ext.FileInfo;
import com.zhangfuxing.tools.gateway.ext.RequestInfo;
import com.zhangfuxing.tools.gateway.util.Template;
import com.zhangfuxing.tools.gateway.util.URLUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author 张福兴
 * @version 1.0
 * @date 2025/7/28
 * @email zhangfuxing1010@163.com
 */
public class GatewayHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private static final Logger logger = LoggerFactory.getLogger(GatewayHandler.class);
	private final DynamicConfigManager configManager;

	public GatewayHandler(DynamicConfigManager configManager) {
		this.configManager = configManager;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
		// 处理 CORS 预检请求
		if (request.method() == HttpMethod.OPTIONS) {
			FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
			response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
			response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");
			response.headers().set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "3600");
			response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
			ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
			return;
		}

		String path = request.uri();
		RouteConfig route = configManager.getRouteForPath(path);

		if (route == null) {
			logger.info("未找到相关的路由配置，无法代理目标：{}", request.uri());
			sendError(ctx, HttpResponseStatus.NOT_FOUND, "No route found for path: " + path);
			return;
		}

		try {
			// 静态资源路由
			if (route.resourceRoute()) {
				handleResourceRequest(route, ctx, request);
				return;
			}

			// 构建转发URL
			String targetUrl = buildTargetUrl(route, request);

			// 创建转发请求
			FullHttpRequest proxyRequest = createProxyRequest(request, targetUrl);

			// 应用路由过滤器
			applyFilters(route, proxyRequest);

			// 执行转发
			executeProxyRequest(ctx, proxyRequest, route, request);
		} catch (Exception e) {
			logger.error("Error processing request", e);
			sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
		}
	}

	private String buildTargetUrl(RouteConfig route, FullHttpRequest request) {
		return route.routingUrl(request);
	}

	private FullHttpRequest createProxyRequest(FullHttpRequest original, String targetUrl) {
		// 创建新的请求对象
		FullHttpRequest newRequest = new DefaultFullHttpRequest(
				original.protocolVersion(),
				original.method(),
				URLUtils.getPathAndParams(targetUrl),
				original.content().retainedDuplicate()
		);

		// 复制请求头
		newRequest.headers().setAll(original.headers());

		// 移除一些不应该转发的头部
		newRequest.headers().remove(HttpHeaderNames.CONNECTION);
		newRequest.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
		// 更新Host头
		try {
			URI uri = new URI(targetUrl);
			newRequest.headers().set(HttpHeaderNames.HOST, uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : ""));
		} catch (URISyntaxException e) {
			logger.warn("Invalid target URL: {}", targetUrl);
		}

		return newRequest;
	}

	private void applyFilters(RouteConfig route, FullHttpRequest request) {
		// 应用配置的过滤器
		for (Map.Entry<String, String> filter : route.getFilters().entrySet()) {
			String filterName = filter.getKey();
			String filterConfig = filter.getValue();

			// 实现各种过滤器
			switch (filterName) {
				case "AddRequestHeader" -> {
					String[] parts = filterConfig.split("=", 2);
					if (parts.length == 2) {
						request.headers().add(parts[0], parts[1]);
					}
				}
				case "SetHostHeader" -> request.headers().set(HttpHeaderNames.HOST, filterConfig);
				case "StripPrefix" -> {
					int prefixCount = Integer.parseInt(filterConfig);
					String uri = request.uri();
					for (int i = 0; i < prefixCount; i++) {
						int nextSlash = uri.indexOf('/', 1);
						if (nextSlash > 0) {
							uri = uri.substring(nextSlash);
						} else {
							uri = "/";
							break;
						}
					}
					request.setUri(uri);
				}
			}
		}
	}

	private void executeProxyRequest(ChannelHandlerContext ctx, FullHttpRequest request, RouteConfig route, FullHttpRequest originalRequest) {
		// 解析目标主机和端口
		URI uri;
		String target = route.getTarget();
		try {
			uri = URI.create(target);
		} catch (Exception e) {
			sendError(ctx, HttpResponseStatus.BAD_GATEWAY, "Invalid target URI: " + target);
			return;
		}

		String host = uri.getHost();
		int port = uri.getPort() > 0 ? uri.getPort() :
				"https".equals(uri.getScheme()) ? 443 : 80;

		// 创建Bootstrap来建立到目标服务器的连接
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(ctx.channel().eventLoop())
				.channel(ctx.channel().getClass())
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
				.handler(new ChannelInitializer<>() {
					@Override
					protected void initChannel(Channel ch) {
						ch.pipeline()
								.addLast(new HttpClientCodec())
								.addLast(new HttpObjectAggregator(1024 * 1024))
								.addLast(new HttpResponseHandler(ctx, route, request, originalRequest));
					}
				});

		bootstrap.connect(host, port)
				.addListener((ChannelFuture future) -> {
					String originUrl = Template.engineFmt("${method} ${protocol}://${host}${uri}", new RequestInfo(originalRequest));
					String targetUrl = Template.engineFmt("${method} ${protocol}://${host}${uri}", new RequestInfo(request));
					if (future.isSuccess()) {
						Channel proxyChannel = future.channel();
						proxyChannel.writeAndFlush(request)
								.addListener((ChannelFuture writeFuture) -> {
									if (!writeFuture.isSuccess()) {
										closeOnFlush(proxyChannel);
										sendError(ctx, HttpResponseStatus.BAD_GATEWAY, "Proxy connection failed");
									}
								});
					} else {
						logger.error("{} ==> {} | ERROR: {}", originUrl, targetUrl, future.cause().getMessage());
						sendError(ctx, HttpResponseStatus.BAD_GATEWAY, "Cannot connect to target service");
					}
				});
	}

	private static class HttpResponseHandler extends ChannelInboundHandlerAdapter {
		static final Logger logger = LoggerFactory.getLogger(HttpResponseHandler.class);
		private final ChannelHandlerContext clientContext;
		private final RouteConfig route;
		private final FullHttpRequest request;
		private final FullHttpRequest originalRequest;

		public HttpResponseHandler(ChannelHandlerContext clientContext, RouteConfig route, FullHttpRequest request, FullHttpRequest originalRequest) {
			this.clientContext = clientContext;
			this.route = route;
			this.request = request;
			this.originalRequest = originalRequest;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			String originUrl = Template.engineFmt("${method} ${protocol}://${host}${uri}", new RequestInfo(originalRequest));
			String targetUrl = Template.engineFmt("${method} ${protocol}://${host}${uri}", new RequestInfo(request));
			if (msg instanceof FullHttpResponse response) {
				// 添加 CORS 头部
				response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
				response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
				response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");
				Map<String, String> responseHeaders = route.getResponseHeaders();
				if (responseHeaders != null && !responseHeaders.isEmpty()) {
					responseHeaders.forEach((k,v) -> response.headers().set(k, v));
				}
				logger.info("{} => {} | status: {}", originUrl, targetUrl, response.status().code());
				// 将响应写回客户端，使用retainedDuplicate确保引用计数正确
				clientContext.writeAndFlush(response.retainedDuplicate())
						.addListener(ChannelFutureListener.CLOSE);
			} else {
				logger.warn("{}  ==>  {} | {}: {}", originUrl, targetUrl, "ERROR", "Invalid message type");
				// 释放不处理的消息
				ReferenceCountUtil.release(msg);
			}
			closeOnFlush(ctx.channel());
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			logger.error("Proxy response error", cause);
			closeOnFlush(ctx.channel());
		}
	}

	private static void closeOnFlush(Channel ch) {
		if (ch.isActive()) {
			ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
		FullHttpResponse response = new DefaultFullHttpResponse(
				HttpVersion.HTTP_1_1, status,
				Unpooled.copiedBuffer(message, CharsetUtil.UTF_8));
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

		// 添加 CORS 头部到错误响应
		response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
		response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");

		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.error("Gateway error", cause);
		sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Gateway internal error");
		closeOnFlush(ctx.channel());
	}

	private void handleResourceRequest(RouteConfig route, ChannelHandlerContext ctx, FullHttpRequest request) {
		String baseDir = route.getRoot();
		String index = route.getIndex();

		String uri = route.routingUrl(URI.create(request.uri()).getPath());
		if ("/".equals(uri) || uri.isBlank()) {
			uri = index;
		}
		String filepath = pathJoin(baseDir, URLDecoder.decode(uri, StandardCharsets.UTF_8));
		Path resourcePath = Paths.get(filepath);

		// 检查文件是否存在
		if (!Files.exists(resourcePath)) {
			logger.error("resource not found: {}", filepath);
			sendError(ctx, HttpResponseStatus.NOT_FOUND, "resource not found: " + filepath);
			return;
		}
		if (Files.isDirectory(resourcePath)) {
			writeDirs1(ctx, resourcePath, Paths.get(baseDir));
			return;
		} else if (!Files.isRegularFile(resourcePath)) {
			sendError(ctx, HttpResponseStatus.NOT_FOUND, "resource not found: " + filepath);
			return;
		}


		try {
			RandomAccessFile raf = new RandomAccessFile(resourcePath.toFile(), "r");
			long fileLength = Files.size(resourcePath);

			// HEAD 请求只发送头部
			if (request.method() == HttpMethod.HEAD) {
				FullHttpResponse response = new DefaultFullHttpResponse(
						HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				response.headers().set(HttpHeaderNames.CONTENT_LENGTH, fileLength);
				response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				return;
			}

			if (!ctx.channel().isActive()) {
				logger.warn("Channel is not active, aborting file transfer: {}", filepath);
				return;
			}
			String query = URI.create(request.uri()).getQuery();
			HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
			String contentType = FileInfo.getContentType(filepath, query);
			HttpUtil.setContentLength(response, fileLength);
			response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
			if ("application/octet-stream".equals(contentType)) {
				response.headers()
						.set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"" +
																  URLEncoder.encode(resourcePath.getFileName().toString(), StandardCharsets.UTF_8) + "\"");
			}

			if (ctx.pipeline().get(SslHandler.class) != null) {
				ctx.write(response);
			} else {
				ctx.write(response, ctx.voidPromise());
			}
			ChunkedFile chunkedFile = new ChunkedFile(raf, 8192);
			ctx.writeAndFlush(chunkedFile)
					.addListener(future -> raf.close())
					.addListener(ChannelFutureListener.CLOSE);


		} catch (IOException e) {
			logger.error("Error serving resource: {}", filepath, e);
			sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
		}
	}

	private static void writeDirs1(ChannelHandlerContext ctx, Path resourcePath, Path rootPath) {
		List<FileInfo> list = new ArrayList<>();
		try (Stream<Path> pathStream = Files.list(resourcePath)) {
			pathStream.map(Path::toFile)
					.filter(f -> !f.isHidden())
					.forEach(file -> list.add(new FileInfo(file, rootPath)));
		} catch (IOException e) {
			sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error reading directory");
		}
		try {
			var html = Files.readString(Paths.get("./web/file-browse.ftl"));
			FullHttpResponse response = new DefaultFullHttpResponse(
					HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
					Unpooled.copiedBuffer(Template.engineFmt(html, Map.of("files", list)), StandardCharsets.UTF_8));
			response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
			response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
			response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");
			ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String pathJoin(String... paths) {
		var result = new StringBuilder();
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i].replace("\\", "/");
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			if (i == 0) {
				result.append(path);
				continue;
			}
			result.append("/").append(path);
		}
		return result.toString();
	}

}
