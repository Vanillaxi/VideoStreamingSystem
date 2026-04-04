package VO;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String token;//登录成功后，顺带返回 JWT

}
