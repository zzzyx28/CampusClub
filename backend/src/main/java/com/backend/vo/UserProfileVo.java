package com.backend.vo;

import com.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVo {

    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Integer userType;
    private Integer memberLevel;
    private String memberLevelLabel;

    public static UserProfileVo from(User user) {
        if (user == null) {
            return null;
        }
        return new UserProfileVo(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getUserType(),
                user.getMemberLevel(),
                memberLevelLabel(user.getMemberLevel()));
    }

    private static String memberLevelLabel(Integer level) {
        if (level == null) {
            return "新成员";
        }
        return switch (level) {
            case 2 -> "活跃";
            case 3 -> "骨干";
            case 4 -> "核心";
            case 5 -> "荣誉";
            default -> "新成员";
        };
    }
}
