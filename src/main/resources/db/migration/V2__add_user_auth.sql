-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt加密',
    nickname    VARCHAR(50),
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'ADMIN / USER',
    status      INT          NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- game_account 表增加 user_id 列
ALTER TABLE game_account ADD COLUMN user_id BIGINT DEFAULT NULL COMMENT '归属用户ID';
