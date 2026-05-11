package com.deltaforce.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.deltaforce.manager.constant.GameMonitorConstants;
import com.deltaforce.manager.dto.ManualBalanceRequest;
import com.deltaforce.manager.dto.Result;
import com.deltaforce.manager.entity.GameBalanceRecord;
import com.deltaforce.manager.service.IGameBalanceRecordService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;

@RestController
@RequestMapping("/api/balance")
@Slf4j
public class BalanceRecordController {

    @Resource
    private IGameBalanceRecordService balanceRecordService;

    @GetMapping("/list")
    public Result<IPage<GameBalanceRecord>> queryPageList(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "accountId", required = false) Long accountId) {

        LambdaQueryWrapper<GameBalanceRecord> wrapper = new LambdaQueryWrapper<>();
        if (accountId != null) {
            wrapper.eq(GameBalanceRecord::getAccountId, accountId);
        }
        wrapper.orderByDesc(GameBalanceRecord::getRecordTime);

        Page<GameBalanceRecord> page = new Page<>(pageNo, pageSize);
        IPage<GameBalanceRecord> pageList = balanceRecordService.page(page, wrapper);
        return Result.OK(pageList);
    }

    @PostMapping("/manualInput")
    public Result<String> manualInput(@RequestBody ManualBalanceRequest request) {
        GameBalanceRecord record = new GameBalanceRecord();
        record.setAccountId(request.getAccountId());
        record.setBalance(request.getBalance());
        record.setBalanceChange(balanceRecordService.computeBalanceChange(request.getAccountId(), request.getBalance()));
        record.setRecordTime(new Date());
        record.setSource(GameMonitorConstants.SOURCE_MANUAL);
        record.setCreateTime(new Date());
        balanceRecordService.save(record);
        return Result.OK("录入成功!");
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("id") Long id) {
        balanceRecordService.removeById(id);
        return Result.OK("删除成功!");
    }
}
