package com.zhangfuxing.tools.gateway.util;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

/**
 * @author 张福兴
 * @version 1.0
 * @date 2025/8/4
 * @email zhangfuxing1010@163.com
 */
public class URLUtils {

	public static String getPathAndParams(String url) {
		URI uri = URI.create(url);
		String path = uri.getPath();
		String query = uri.getQuery();
		if (query != null) {
			StringJoiner joiner = new StringJoiner("&");
			for (String param : query.split("&")) {
				String[] parts = param.split("=");
				joiner.add(parts[0] + "=" + URLEncoder.encode(parts[1], StandardCharsets.UTF_8));
			}
			query = "?" + joiner;
		} else {
			query = "";
		}
		return path + query;
	}
}
