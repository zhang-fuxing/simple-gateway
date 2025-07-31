package com.zhangfuxing.tools.gateway.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author 张福兴
 * @version 1.0
 * @date 2025/7/28
 * @email zhangfuxing1010@163.com
 */
public class DynamicGateway {
	private static final Logger logger = LoggerFactory.getLogger(DynamicGateway.class);
	private final List<Integer> ports = new ArrayList<>();
	private static final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	private final Map<Integer, DynamicConfigManager> configManagers;

	public DynamicGateway(DynamicConfigManager... configManagers) {
		this.configManagers = new HashMap<>(configManagers.length, 1.0f);
		for (DynamicConfigManager configManager : configManagers) {
			this.configManagers.put(configManager.getBindPort(), configManager);
			this.ports.add(configManager.getBindPort());
		}
	}

	public void start() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			List<ChannelFuture> channelFutures = new ArrayList<>();

			// 为每个端口创建一个ServerBootstrap
			for (int port : ports) {
				DynamicConfigManager configManager = configManagers.get(port);
				if (configManager == null) {
					continue;
				}
				ServerBootstrap b = new ServerBootstrap();
				b.group(bossGroup, workerGroup)
						.channel(NioServerSocketChannel.class)
						.handler(new LoggingHandler(LogLevel.INFO))
						.childHandler(new ChannelInitializer<SocketChannel>() {
							@Override
							protected void initChannel(SocketChannel ch) {
								ChannelPipeline pipeline = ch.pipeline();
								pipeline.addLast(new ChunkedWriteHandler());
								// HTTP编解码器
								pipeline.addLast(new HttpServerCodec());
								// 聚合HTTP请求
								pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
								// 自定义网关处理器
								pipeline.addLast(new GatewayHandler(configManager));
								pipeline.addLast(new LoggingHandler(LogLevel.ERROR));
								// 将channel添加到组中，便于统一管理
								allChannels.add(ch);
							}
						})
						.option(ChannelOption.SO_BACKLOG, 128)
						.childOption(ChannelOption.SO_KEEPALIVE, true);

				ChannelFuture f = b.bind(port);
				channelFutures.add(f);
			}

			// 等待所有端口绑定完成
			for (int i = 0; i < channelFutures.size(); i++) {
				try {
					ChannelFuture f = channelFutures.get(i);
					f.sync();
					allChannels.add(f.channel());
					logger.info("端口：{} 已绑定", ports.get(i));
				} catch (Exception e) {
					logger.error("端口：{} 绑定失败,{}", ports.get(i), e.getMessage());
				}
			}
			// 注册一个关闭钩子，用于优雅关闭
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				logger.info("Shutting down gateway...");
				try {
					allChannels.close().sync();
					bossGroup.shutdownGracefully();
					workerGroup.shutdownGracefully();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				logger.info("Gateway shutdown complete.");
			}));
			// 等待任意一个channel关闭
			allChannels.newCloseFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

}
