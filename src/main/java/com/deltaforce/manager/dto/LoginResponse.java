package com.deltaforce.manager.dto;

import com.deltaforce.manager.entity.SysUser;
import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private SysUser userInfo;
}
