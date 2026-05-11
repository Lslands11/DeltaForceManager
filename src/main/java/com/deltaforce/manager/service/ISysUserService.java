package com.deltaforce.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.deltaforce.manager.entity.SysUser;

public interface ISysUserService extends IService<SysUser> {
    SysUser getByUsername(String username);
}
