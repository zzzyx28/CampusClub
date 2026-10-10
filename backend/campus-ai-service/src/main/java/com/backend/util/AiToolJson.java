package com.backend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具统一返回 JSON字符串，便于模型解析 success / message，避免 null 与空列表语义不清。
 */
public final class AiToolJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiToolJson() {
    }

    public static String ok(String message, Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("message", message);
        m.put("data", data);
        return toJson(m);
    }

    public static String fail(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("message", message);
        m.put("data", null);
        return toJson(m);
    }

    private static String toJson(Map<String, Object> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"success\":false,\"message\":\"JSON序列化失败\",\"data\":null}";
        }
    }
}
