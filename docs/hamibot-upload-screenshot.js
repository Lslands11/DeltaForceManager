/**
 * 游戏余额截图自动上传脚本（监听模式）
 * 运行环境: Hamibot / Auto.js
 *
 * 触发方式: 监听游戏 App 启动，自动执行截图上传
 *
 * 使用说明:
 *   1. 修改下方 STATIC_CONFIG 中的 serverUrl 和 packageName
 *   2. 首次运行会弹出配置界面，输入 deviceToken 并保存
 *   3. 脚本常驻后台，当检测到游戏被打开时自动执行
 *   4. 不需要设置定时任务，只需要保持脚本在后台运行
 */

// ==================== 静态配置（所有设备相同）====================
var STATIC_CONFIG = {
    // 服务器地址
    serverUrl: "http://192.168.1.100:8080",

    // 游戏包名（用"应用包名查看器"获取）
    packageName: "com.example.game",

    // 游戏启动后等待加载的时间（秒）
    gameLaunchWait: 30,

    // 截图保存路径
    screenshotPath: "/sdcard/hamibot_balance.png",

    // 两次执行之间的冷却时间（秒），防止重复触发
    cooldownSeconds: 60,

    // 是否开启调试日志
    debug: true
};
// =================================================================

// ==================== 本地存储管理 ====================

var STORAGE_KEY = "balance_monitor_config";

function loadConfig() {
    try {
        var stored = hamibot.storage.get(STORAGE_KEY);
        if (stored) return JSON.parse(stored);
    } catch(e) {}
    try {
        var s = storages.create("balance_monitor");
        var stored = s.get(STORAGE_KEY);
        if (stored) return typeof stored === "string" ? JSON.parse(stored) : stored;
    } catch(e) {}
    return null;
}

function saveConfig(config) {
    var json = JSON.stringify(config);
    try {
        hamibot.storage.put(STORAGE_KEY, json);
        return;
    } catch(e) {}
    try {
        var s = storages.create("balance_monitor");
        s.put(STORAGE_KEY, config);
    } catch(e) {
        log("保存配置失败: " + e);
    }
}

function showConfigDialog() {
    var saved = loadConfig() || {};
    var existingToken = saved.deviceToken || "";

    var input = dialogs.input(
        "设备配置",
        "请输入 deviceToken（在后台管理系统的账号列表中获取）:\n\n" +
        "当前: " + (existingToken ? "已设置(..." + existingToken.slice(-6) + ")" : "未设置"),
        existingToken
    );

    if (input !== null && input !== undefined && input.trim() !== "") {
        saved.deviceToken = input.trim();
        saveConfig(saved);
        toastMsg("token 已保存");
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

/**
 * 关闭弹窗（签到、广告等）
 */
function closePopups() {
    log("尝试关闭弹窗");
    var closeTexts = [
        "关闭", "跳过", "取消", "我知道了", "稍后再说",
        "不再提示", "关闭广告", "领取", "签到",
        "今日不再提示", "以后再说", "暂不", "忽略"
    ];

    for (var i = 0; i < closeTexts.length; i++) {
        var btn = text(closeTexts[i]).findOne(1000);
        if (btn) {
            log("关闭弹窗: " + closeTexts[i]);
            btn.click();
            sleep(1000);
        }
    }
}

/**
 * 导航到余额界面
 * 【重要】根据实际游戏界面修改
 */
function navigateToBalance() {
    log("导航到余额界面");
    var balanceTexts = ["余额", "资产", "金币", "游戏币", "背包", "仓库"];

    for (var i = 0; i < balanceTexts.length; i++) {
        var target = text(balanceTexts[i]).findOne(2000);
        if (target) {
            log("找到按钮: " + balanceTexts[i]);
            target.click();
            sleep(2000);
            return true;
        }
    }
    log("未找到余额按钮");
    return false;
}

/**
 * 截图
 */
function takeBalanceScreenshot() {
    log("截图");
    sleep(1500);

    var path = STATIC_CONFIG.screenshotPath;
    if (files.exists(path)) files.remove(path);

    captureScreen(path);
    sleep(500);

    if (files.exists(path)) {
        log("截图成功");
        return path;
    }
    log("截图失败");
    return null;
}

/**
 * 上传截图
 */
function uploadScreenshot(filePath, deviceToken) {
    log("上传截图");
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
            return true;
        }
        log("上传失败: " + (json.message || body));
        return false;
    } catch (e) {
        log("上传异常: " + e.message);
        return false;
    }
}

/**
 * 执行一次完整的截图上传流程
 */
function executeTask(deviceToken) {
    log("========== 开始执行截图上传 ==========");

    // 等待游戏加载完成
    log("等待游戏加载 " + STATIC_CONFIG.gameLaunchWait + " 秒...");
    sleep(STATIC_CONFIG.gameLaunchWait * 1000);

    // 关闭弹窗
    closePopups();
    sleep(1000);

    // 导航到余额
    if (!navigateToBalance()) {
        toastMsg("未找到余额界面");
        back();
        sleep(500);
        home();
        return false;
    }

    // 再次关闭弹窗
    closePopups();

    // 截图
    var screenshotPath = takeBalanceScreenshot();
    if (!screenshotPath) {
        toastMsg("截图失败");
        home();
        return false;
    }

    // 上传
    var uploaded = uploadScreenshot(screenshotPath, deviceToken);
    if (uploaded) {
        toastMsg("余额截图上传成功");
    } else {
        toastMsg("上传失败");
    }

    // 返回桌面
    home();
    sleep(500);

    log("========== 执行完成 ==========");
    return uploaded;
}

function backToHome() {
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

// ==================== 主流程（监听模式）====================

function main() {
    log("========== 监听服务启动 ==========");

    // 读取或配置 token
    var dynamicConfig = loadConfig();
    if (!dynamicConfig || !dynamicConfig.deviceToken) {
        dynamicConfig = showConfigDialog();
    }
    if (!dynamicConfig || !dynamicConfig.deviceToken) {
        toastMsg("未配置 deviceToken，无法启动监听");
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

    // 防重复触发：记录上次执行时间
    var lastRunTime = 0;

    toastMsg("监听已启动，打开游戏时将自动截图上传");

    // === 监听模式：检测前台应用 ===
    events.observeApp();

    events.on("app_launched", function(packageName) {
        // 只响应目标游戏
        if (packageName !== STATIC_CONFIG.packageName) {
            return;
        }

        // 冷却检查，防止重复触发
        var now = new Date().getTime();
        if (now - lastRunTime < STATIC_CONFIG.cooldownSeconds * 1000) {
            log("冷却中，跳过本次触发");
            return;
        }
        lastRunTime = now;

        log("检测到游戏启动: " + packageName);
        executeTask(deviceToken);
    });

    // 保持脚本运行
    setInterval(function() {
        // 心跳，保持后台存活
    }, 60000);
}

main();
