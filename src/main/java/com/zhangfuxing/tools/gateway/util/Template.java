package com.zhangfuxing.tools.gateway.util;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

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

	public static String format(String template, Object data) {
		try (StringWriter writer = new StringWriter()){
			var t = new freemarker.template.Template("inline_template", template, cfg);
			t.process(data, writer);
			return writer.toString();
		} catch (IOException | TemplateException e) {
			throw new RuntimeException("Failed to process template: " + template, e);
		}
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
