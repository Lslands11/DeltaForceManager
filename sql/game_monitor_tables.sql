-- =============================================
-- 手游搬砖余额监控系统 DDL
-- =============================================

-- 游戏账号表
CREATE TABLE IF NOT EXISTS `game_account` (
  `id`            bigint       NOT NULL  COMMENT '主键',
  `account_name`  varchar(64)  NOT NULL  COMMENT '账号名称/昵称',
  `game_name`     varchar(64)  DEFAULT NULL COMMENT '游戏名称',
  `device_id`     varchar(128) NOT NULL  COMMENT '设备唯一标识',
  `device_token`  varchar(128) NOT NULL  COMMENT '上传鉴权Token',
  `device_model`  varchar(64)  DEFAULT NULL COMMENT '设备型号',
  `status`        tinyint      NOT NULL DEFAULT 1 COMMENT '状态: 1=启用 0=禁用',
  `remark`        varchar(255) DEFAULT NULL COMMENT '备注',
  `create_by`     varchar(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT NULL COMMENT '更新人',
  `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_token` (`device_token`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='游戏账号';

-- OCR配置表（每个账号独立的截图裁剪坐标和OCR参数）
CREATE TABLE IF NOT EXISTS `game_ocr_config` (
  `id`              bigint       NOT NULL  COMMENT '主键',
  `account_id`      bigint       NOT NULL  COMMENT '关联游戏账号ID',
  `crop_x`          int          NOT NULL DEFAULT 0    COMMENT '裁剪起始X坐标',
  `crop_y`          int          NOT NULL DEFAULT 0    COMMENT '裁剪起始Y坐标',
  `crop_width`      int          NOT NULL DEFAULT 200  COMMENT '裁剪宽度',
  `crop_height`     int          NOT NULL DEFAULT 60   COMMENT '裁剪高度',
  `threshold_value` int          NOT NULL DEFAULT 128  COMMENT '二值化阈值(0-255)',
  `invert_colors`   tinyint      NOT NULL DEFAULT 0    COMMENT '是否反转颜色: 1=是 0=否',
  `scale_factor`    decimal(3,1) NOT NULL DEFAULT 2.0  COMMENT '放大倍数',
  `tesseract_psm`   int          NOT NULL DEFAULT 7    COMMENT 'Tesseract PSM模式(7=单行文本)',
  `unit_suffix`     varchar(16)  DEFAULT '万'          COMMENT '金额单位后缀(万/K/M等)',
  `create_time`     datetime     DEFAULT NULL           COMMENT '创建时间',
  `update_time`     datetime     DEFAULT NULL           COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='OCR配置';

-- 截图日志表
CREATE TABLE IF NOT EXISTS `game_screenshot_log` (
  `id`              bigint        NOT NULL  COMMENT '主键',
  `account_id`      bigint        NOT NULL  COMMENT '游戏账号ID',
  `original_url`    varchar(512)  NOT NULL  COMMENT '原始截图OSS路径',
  `cropped_url`     varchar(512)  DEFAULT NULL COMMENT '裁剪后图片OSS路径',
  `upload_time`     datetime      NOT NULL  COMMENT '上传时间',
  `ocr_status`      tinyint       NOT NULL DEFAULT 0 COMMENT 'OCR状态: 0=待处理 1=成功 2=失败 3=低置信度待审核',
  `ocr_raw_text`    varchar(128)  DEFAULT NULL COMMENT 'OCR原始识别文本',
  `ocr_confidence`  decimal(5,2)  DEFAULT NULL COMMENT 'OCR置信度(0-100)',
  `parsed_amount`   decimal(18,2) DEFAULT NULL COMMENT '解析后的金额数值',
  `manual_amount`   decimal(18,2) DEFAULT NULL COMMENT '人工审核修正金额',
  `error_message`   varchar(512)  DEFAULT NULL COMMENT '错误信息',
  `ocr_process_time` int          DEFAULT NULL COMMENT 'OCR处理耗时(毫秒)',
  `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_upload_time` (`upload_time`),
  KEY `idx_ocr_status` (`ocr_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='截图日志';

-- 余额记录表（最终确认的余额数据）
CREATE TABLE IF NOT EXISTS `game_balance_record` (
  `id`                bigint        NOT NULL  COMMENT '主键',
  `account_id`        bigint        NOT NULL  COMMENT '游戏账号ID',
  `screenshot_log_id` bigint        DEFAULT NULL COMMENT '关联截图日志ID',
  `balance`           decimal(18,2) NOT NULL  COMMENT '余额数值',
  `balance_change`    decimal(18,2) DEFAULT NULL COMMENT '相比上一条记录的变化量',
  `record_time`       datetime      NOT NULL  COMMENT '记录时间(截图时间)',
  `source`            tinyint       NOT NULL DEFAULT 1 COMMENT '来源: 1=OCR自动 2=手动录入 3=人工修正',
  `create_time`       datetime      DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_account_time` (`account_id`, `record_time`),
  KEY `idx_record_time` (`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='余额记录';
