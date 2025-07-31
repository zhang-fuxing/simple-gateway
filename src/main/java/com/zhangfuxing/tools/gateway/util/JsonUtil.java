package com.zhangfuxing.tools.gateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author 张福兴
 * @version 1.0
 * @date 2025/7/29
 * @email zhangfuxing1010@163.com
 */
public class JsonUtil {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static <T> T toBean(String jsonStr, Class<T> clazz) {
		try {
			return JsonUtil.OBJECT_MAPPER.convertValue(JsonUtil.OBJECT_MAPPER.readTree(jsonStr), clazz);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String format(Object object) {
		try {
			return JsonUtil.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
