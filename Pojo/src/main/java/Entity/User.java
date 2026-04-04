package Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private Integer role;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

}
