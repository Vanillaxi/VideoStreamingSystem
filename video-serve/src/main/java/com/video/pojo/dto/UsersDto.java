package com.video.pojo.dto;


import com.video.pojo.entity.User;
import lombok.Data;
import java.util.List;

@Data
public class UsersDto {
    Long count;
    List<User> users;
}
