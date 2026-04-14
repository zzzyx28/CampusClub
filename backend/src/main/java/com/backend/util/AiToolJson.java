package com.backend.util;

import com.alibaba.fastjson.JSON;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具统一返回 JSON字符串，便于模型解析 success / message，避免 null 与空列表语义不清。
 */
public final class AiToolJson {

    private AiToolJson() {
    }

    public static String ok(String message, Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("message", message);
        m.put("data", data);
        return JSON.toJSONString(m);
    }

    public static String fail(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("message", message);
        m.put("data", null);
        return JSON.toJSONString(m);
    }
}
