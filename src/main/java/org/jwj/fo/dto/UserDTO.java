package org.jwj.fo.dto;

import lombok.Data;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        // 手动将id转换为String
        map.put("id", String.valueOf(id));
        map.put("nickName", nickName);
        map.put("icon", icon);
        return map;
    }
}
