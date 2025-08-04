package com.zhangfuxing.tools.gateway.util;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author 张福兴
 * @version 1.0
 * @date 2025/7/31
 * @email zhangfuxing1010@163.com
 */
public class Template {
	private static volatile Configuration cfg;

	static {
		init();
	}

	/**
	 * 使用FreeMarker模板引擎格式化字符串
	 *
	 * @param template 模板字符串
	 * @param data     数据对象
	 * @return 格式化后的字符串
	 */
	public static String engineFmt(String template, Object data) {
		try (StringWriter writer = new StringWriter()) {
			var t = new freemarker.template.Template("inline_template", template, cfg);
			t.process(data, writer);
			return writer.toString();
		} catch (IOException | TemplateException e) {
			throw new RuntimeException("Failed to process template: " + template, e);
		}
	}

	/**
	 * 使用SLF4J风格的占位符格式化字符串，支持{}占位符
	 *
	 * @param messagePattern 包含占位符的消息模板
	 * @param argArray       参数数组
	 * @return 格式化后的字符串
	 */
	public static String fmtStr(String messagePattern, Object... argArray) {
		return MessageFormatter.basicArrayFormat(messagePattern, argArray);

	}

	private static void init() {
		if (cfg == null) {
			synchronized (freemarker.template.Template.class) {
				if (cfg == null) {
					cfg = new Configuration(Configuration.VERSION_2_3_22);
					cfg.setDefaultEncoding("UTF-8");
					cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
				}
			}
		}
	}
}
