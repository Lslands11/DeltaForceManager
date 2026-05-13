package com.deltaforce.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.deltaforce.manager.dto.Result;
import com.deltaforce.manager.entity.GameAccount;
import com.deltaforce.manager.entity.GameOcrConfig;
import com.deltaforce.manager.service.IGameAccountService;
import com.deltaforce.manager.service.IGameOcrConfigService;
import com.deltaforce.manager.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
@Slf4j
public class GameAccountController {

    @Resource
    private IGameAccountService gameAccountService;
    @Resource
    private IGameOcrConfigService ocrConfigService;

    @GetMapping("/list")
    public Result<IPage<GameAccount>> queryPageList(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "accountName", required = false) String accountName,
            @RequestParam(name = "status", required = false) Integer status) {

        LambdaQueryWrapper<GameAccount> wrapper = new LambdaQueryWrapper<>();
        if (accountName != null && !accountName.isEmpty()) {
            wrapper.like(GameAccount::getAccountName, accountName);
        }
        if (status != null) {
            wrapper.eq(GameAccount::getStatus, status);
        }
        // 非管理员只能看到自己名下的账号
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            wrapper.eq(GameAccount::getUserId, userId);
        }
        wrapper.orderByDesc(GameAccount::getCreateTime);

        Page<GameAccount> page = new Page<>(pageNo, pageSize);
        IPage<GameAccount> pageList = gameAccountService.page(page, wrapper);
        return Result.OK(pageList);
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody GameAccount gameAccount) {
        if (gameAccount.getDeviceToken() == null || gameAccount.getDeviceToken().isEmpty()) {
            gameAccount.setDeviceToken(gameAccountService.generateDeviceToken());
        }
        if (gameAccount.getDeviceId() == null || gameAccount.getDeviceId().isEmpty()) {
            gameAccount.setDeviceId(gameAccountService.generateDeviceId(gameAccount.getDeviceModel()));
        }
        // 非管理员添加账号时自动绑定到当前用户
        if (!SecurityUtil.isAdmin() && gameAccount.getUserId() == null) {
            gameAccount.setUserId(SecurityUtil.getCurrentUserId());
        }
        gameAccountService.save(gameAccount);
        return Result.OK("添加成功！");
    }

    @PutMapping("/edit")
    public Result<String> edit(@RequestBody GameAccount gameAccount) {
        gameAccountService.updateById(gameAccount);
        return Result.OK("编辑成功!");
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("id") Long id) {
        gameAccountService.removeById(id);
        return Result.OK("删除成功!");
    }

    @GetMapping("/queryById")
    public Result<GameAccount> queryById(@RequestParam("id") Long id) {
        GameAccount gameAccount = gameAccountService.getById(id);
        if (gameAccount == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(gameAccount);
    }

    @PostMapping("/generateToken")
    public Result<Map<String, String>> generateToken() {
        String token = gameAccountService.generateDeviceToken();
        Map<String, String> result = new HashMap<>(1);
        result.put("deviceToken", token);
        return Result.OK(result);
    }

    @GetMapping("/ocrConfig")
    public Result<GameOcrConfig> getOcrConfig(@RequestParam("accountId") Long accountId) {
        GameOcrConfig config = ocrConfigService.getByAccountId(accountId);
        if (config == null) {
            config = new GameOcrConfig();
            config.setAccountId(accountId);
            config.setCropX(0);
            config.setCropY(0);
            config.setCropWidth(200);
            config.setCropHeight(60);
            config.setThresholdValue(128);
            config.setInvertColors(0);
            config.setScaleFactor(new BigDecimal("2.0"));
            config.setTesseractPsm(7);
            config.setUnitSuffix("万");
        }
        return Result.OK(config);
    }

    @PostMapping("/saveOcrConfig")
    public Result<String> saveOcrConfig(@RequestBody GameOcrConfig ocrConfig) {
        GameOcrConfig existing = ocrConfigService.getByAccountId(ocrConfig.getAccountId());
        if (existing != null) {
            ocrConfig.setId(existing.getId());
            ocrConfigService.updateById(ocrConfig);
        } else {
            ocrConfigService.save(ocrConfig);
        }
        return Result.OK("保存成功!");
    }
}
