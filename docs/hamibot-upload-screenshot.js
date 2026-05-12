/**
 * 游戏余额截图自动上传脚本
 * 运行环境: Hamibot / Auto.js
 *
 * 功能: 定时打开游戏 → 进入余额界面 → 截图 → 上传服务器
 *
 * 使用说明:
 *   1. 修改下方 STATIC_CONFIG 中的 serverUrl 和 packageName
 *   2. 首次运行会弹出配置界面，输入 deviceToken 并保存
 *   3. 后续运行自动读取本地存储的配置，无需重复输入
 *   4. 如需修改配置，运行时调用 showConfigDialog() 或删除存储后重新运行
 */

// ==================== 静态配置（所有设备相同）====================
var STATIC_CONFIG = {
    // 服务器地址
    serverUrl: "http://192.168.1.100:8080",

    // 游戏包名（用"应用包名查看器"获取）
    packageName: "com.example.game",

    // 游戏启动等待时间（秒）
    gameLaunchWait: 30,

    // 截图保存路径
    screenshotPath: "/sdcard/hamibot_balance.png",

    // 是否开启调试日志
    debug: true
};
// =================================================================

// ==================== 本地存储管理 ====================

var STORAGE_KEY = "balance_monitor_config";

/**
 * 从本地存储读取动态配置（deviceToken 等）
 */
function loadConfig() {
    try {
        // Hamibot 使用 hamibot.storage
        var stored = hamibot.storage.get(STORAGE_KEY);
        if (stored) {
            log("已加载本地配置");
            return JSON.parse(stored);
        }
    } catch(e) {}

    // Auto.js 使用 storages 模块
    try {
        var s = storages.create("balance_monitor");
        var stored = s.get(STORAGE_KEY);
        if (stored) {
            log("已加载本地配置(Auto.js)");
            return typeof stored === "string" ? JSON.parse(stored) : stored;
        }
    } catch(e) {}

    return null;
}

/**
 * 保存动态配置到本地存储
 */
function saveConfig(config) {
    var json = JSON.stringify(config);
    try {
        hamibot.storage.put(STORAGE_KEY, json);
        log("配置已保存");
        return;
    } catch(e) {}

    try {
        var s = storages.create("balance_monitor");
        s.put(STORAGE_KEY, config);
        log("配置已保存(Auto.js)");
    } catch(e) {
        log("保存配置失败: " + e);
    }
}

/**
 * 弹出配置界面，让用户输入 deviceToken
 * 返回合并后的完整 CONFIG
 */
function showConfigDialog() {
    // 加载已有的配置
    var saved = loadConfig() || {};
    var existingToken = saved.deviceToken || "";

    // 弹出输入框
    var input = dialogs.input(
        "设备配置",
        "请输入 deviceToken（在后台管理系统的账号列表中获取）:\n\n" +
        "当前已有配置: " + (existingToken ? "已设置（末尾..." + existingToken.slice(-6) + "）" : "未设置"),
        existingToken
    );

    if (input === null || input === undefined || input.trim() === "") {
        toast("未输入 token，使用已有配置");
    } else {
        saved.deviceToken = input.trim();
        saveConfig(saved);
        toast("token 已保存");
    }

    return saved;
}

// ==================== 日志工具 ====================
function log(msg) {
    if (STATIC_CONFIG.debug) {
        console.log("[余额监控] " + msg);
    }
}

function toastMsg(msg) {
    log(msg);
    try { toast(msg); } catch(e) {}
}
// ================================================

// ==================== 核心函数 ====================

function launchGame() {
    log("步骤1: 启动游戏 " + STATIC_CONFIG.packageName);
    app.launchApp(STATIC_CONFIG.packageName);
    sleep(STATIC_CONFIG.gameLaunchWait * 1000);
    log("游戏启动等待完成");
}

function closePopups() {
    log("步骤2: 尝试关闭弹窗");

    var closeTexts = [
        "关闭", "跳过", "取消", "我知道了", "稍后再说",
        "不再提示", "关闭广告", "领取", "签到",
        "今日不再提示", "以后再说", "暂不", "忽略"
    ];

    for (var i = 0; i < closeTexts.length; i++) {
        var btn = text(closeTexts[i]).findOne(1000);
        if (btn) {
            log("发现弹窗按钮: " + closeTexts[i] + "，点击关闭");
            btn.click();
            sleep(1000);
        }
    }
}

function navigateToBalance() {
    log("步骤3: 导航到余额界面");

    var balanceTexts = ["余额", "资产", "金币", "游戏币", "背包", "仓库"];

    for (var i = 0; i < balanceTexts.length; i++) {
        var target = text(balanceTexts[i]).findOne(2000);
        if (target) {
            log("找到目标按钮: " + balanceTexts[i]);
            target.click();
            sleep(2000);
            return true;
        }
    }

    log("未找到余额按钮，请检查游戏界面或修改脚本");
    return false;
}

function takeBalanceScreenshot() {
    log("步骤4: 截图");
    sleep(1500);

    var path = STATIC_CONFIG.screenshotPath;
    if (files.exists(path)) {
        files.remove(path);
    }

    captureScreen(path);
    sleep(500);

    if (files.exists(path)) {
        log("截图成功: " + path);
        return path;
    } else {
        log("截图失败");
        return null;
    }
}

function uploadScreenshot(filePath, deviceToken) {
    log("步骤5: 上传截图到服务器");

    var url = STATIC_CONFIG.serverUrl + "/api/device/upload";

    try {
        var res = http.postMultipart(url, {
            device_token: deviceToken,
            screenshot: open(filePath)
        });

        var body = res.body.string();
        log("服务器响应: " + body);

        var json = JSON.parse(body);
        if (json.code === 200 || json.success === true) {
            log("上传成功!");
            return true;
        } else {
            log("上传失败: " + (json.message || body));
            return false;
        }
    } catch (e) {
        log("上传异常: " + e.message);
        return false;
    }
}

function backToHome() {
    log("返回桌面");
    home();
    sleep(500);
}

// ================================================

// ==================== 调试辅助 ====================

function findUI() {
    log("=== 当前界面元素 ===");

    var allTexts = text().find();
    log("文字控件 (" + allTexts.length + " 个):");
    allTexts.forEach(function(widget, index) {
        if (widget.text() && widget.text().trim()) {
            log("  [" + index + "] 文字: \"" + widget.text() + "\"");
        }
    });

    var allClickable = clickable(true).find();
    log("可点击控件 (" + allClickable.length + " 个):");
    allClickable.forEach(function(widget, index) {
        var desc = widget.desc() || "";
        var txt = widget.text() || "";
        var bounds = widget.bounds();
        log("  [" + index + "] 文字:\"" + txt + "\" 描述:\"" + desc +
            "\" 位置:(" + bounds.centerX() + "," + bounds.centerY() + ")");
    });

    log("=== 元素遍历完成 ===");
}

// ================================================

// ==================== 主流程 ====================

function main() {
    log("========== 开始执行 ==========");

    // 读取已保存的动态配置
    var dynamicConfig = loadConfig();

    // 如果没有保存过 token，弹窗让用户输入
    if (!dynamicConfig || !dynamicConfig.deviceToken) {
        dynamicConfig = showConfigDialog();
    }

    if (!dynamicConfig || !dynamicConfig.deviceToken) {
        toastMsg("未配置 deviceToken，无法执行");
        return;
    }

    var deviceToken = dynamicConfig.deviceToken;
    log("使用 token: ..." + deviceToken.slice(-6));

    // 申请截图权限
    if (!requestScreenCapture()) {
        toastMsg("未授予截图权限，无法继续");
        return;
    }
    sleep(1000);

    // 执行主流程
    launchGame();
    closePopups();

    var success = navigateToBalance();
    if (!success) {
        toastMsg("导航到余额界面失败");
        backToHome();
        return;
    }

    closePopups();

    var screenshotPath = takeBalanceScreenshot();
    if (!screenshotPath) {
        toastMsg("截图失败");
        backToHome();
        return;
    }

    var uploaded = uploadScreenshot(screenshotPath, deviceToken);
    if (uploaded) {
        toastMsg("余额截图上传成功");
    } else {
        toastMsg("上传失败，请检查网络");
    }

    backToHome();
    log("========== 执行完成 ==========");
}

// 运行主流程
main();

// ==================== 定时说明 ====================
// Hamibot 中设置定时:
//   脚本页面 → 定时任务 → 添加
//   cron 表达式:
//     0 8 * * *    每天 08:00
//     0 14 * * *   每天 14:00
//     0 22 * * *   每天 22:00
// ================================================
