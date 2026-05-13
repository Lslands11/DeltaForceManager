package com.deltaforce.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.deltaforce.manager.constant.GameMonitorConstants;
import com.deltaforce.manager.entity.GameAccount;
import com.deltaforce.manager.mapper.GameAccountMapper;
import com.deltaforce.manager.service.IGameAccountService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GameAccountServiceImpl extends ServiceImpl<GameAccountMapper, GameAccount> implements IGameAccountService {

    @Override
    public GameAccount getByDeviceToken(String deviceToken) {
        return getOne(new LambdaQueryWrapper<GameAccount>()
                .eq(GameAccount::getDeviceToken, deviceToken)
                .eq(GameAccount::getStatus, GameMonitorConstants.STATUS_ENABLED)
                .last("LIMIT 1"), false);
    }

    @Override
    public String generateDeviceToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String generateDeviceId(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            deviceName = "default";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(deviceName.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return "DN-" + hex.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public List<Long> getAccountIdsByUserId(Long userId) {
        return list(new LambdaQueryWrapper<GameAccount>()
                .eq(GameAccount::getUserId, userId)
                .select(GameAccount::getId))
                .stream()
                .map(GameAccount::getId)
                .collect(Collectors.toList());
    }
}
