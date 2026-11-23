-- =============================================================
-- TVA 模拟人生「人生星轨」数据库初始化脚本
-- 对应《TVA-技术设计文档-V1.0》第三章 数据库设计
-- 使用：mysql -u root -p < sql/schema.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS `tva_startrail`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `tva_startrail`;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  id           BIGINT       NOT NULL COMMENT '主键（雪花）',
  username     VARCHAR(64)  NOT NULL COMMENT '登录账号',
  nickname     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
  email        VARCHAR(128) NOT NULL COMMENT '绑定邮箱（唯一，一邮箱一账号）',
  password     VARCHAR(128) NOT NULL COMMENT '密码哈希（BCrypt）',
  deleted      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_email (email)
) ENGINE=InnoDB COMMENT='用户表';

-- 事件表
CREATE TABLE IF NOT EXISTS `events_info` (
  id           BIGINT       NOT NULL COMMENT '主键（雪花）',
  user_id      BIGINT       NOT NULL COMMENT '所属用户',
  name         VARCHAR(128) NOT NULL COMMENT '事件名称',
  type         TINYINT      NOT NULL COMMENT '主线：10生活/20学业/30事业/40情感',
  visibility   TINYINT      NOT NULL DEFAULT 20 COMMENT '可见性：10公开/20私密（本期默认私密）',
  deleted      TINYINT(1)   NOT NULL DEFAULT 0,
  create_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_user_type (user_id, type)
) ENGINE=InnoDB COMMENT='事件表';

-- 时间节点表
CREATE TABLE IF NOT EXISTS `timeline_node` (
  id           BIGINT        NOT NULL COMMENT '主键（雪花）',
  event_id     BIGINT        NOT NULL COMMENT '所属事件',
  happen_time  DATETIME(3)   NOT NULL COMMENT '发生时间（排序依据）',
  content      VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '节点内容',
  deleted      TINYINT(1)    NOT NULL DEFAULT 0,
  create_time  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_event_time (event_id, happen_time)
) ENGINE=InnoDB COMMENT='时间节点表';

-- 排序约定：节点统一 ORDER BY happen_time ASC, id ASC；
-- 事件不设起止字段，跨度由节点首尾推定（PRD V2.1）
