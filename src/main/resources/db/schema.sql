CREATE DATABASE IF NOT EXISTS health_check_system DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
USE health_check_system;

DROP TABLE IF EXISTS ai_suggestion_cache;
DROP TABLE IF EXISTS physical_data;
DROP TABLE IF EXISTS final_report;
DROP TABLE IF EXISTS examination;
DROP TABLE IF EXISTS doctor_capacity_config;
DROP TABLE IF EXISTS daily_capacity_config;
DROP TABLE IF EXISTS package_item;
DROP TABLE IF EXISTS check_item;
DROP TABLE IF EXISTS check_package;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    role VARCHAR(20) NOT NULL COMMENT '角色: ADMIN / DOCTOR / HR',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用: 1启用 0禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE check_item (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    item_code VARCHAR(20) UNIQUE NOT NULL COMMENT '项目编码',
    item_name VARCHAR(50) NOT NULL COMMENT '项目名称',
    unit VARCHAR(20) DEFAULT NULL COMMENT '单位',
    ref_range VARCHAR(100) DEFAULT NULL COMMENT '参考范围',
    category VARCHAR(50) DEFAULT NULL COMMENT '分类',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用: 1启用 0禁用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检项目表';

CREATE TABLE check_package (
    package_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    package_name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用: 1启用 0禁用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检套餐表';

CREATE TABLE package_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    package_id BIGINT NOT NULL COMMENT '套餐ID',
    item_id BIGINT NOT NULL COMMENT '项目ID',
    CONSTRAINT fk_package FOREIGN KEY (package_id) REFERENCES check_package(package_id) ON DELETE CASCADE,
    CONSTRAINT fk_item FOREIGN KEY (item_id) REFERENCES check_item(item_id) ON DELETE CASCADE,
    UNIQUE KEY uk_package_item (package_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐项目关联表';

CREATE TABLE examination (
    exam_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    real_name VARCHAR(50) NOT NULL COMMENT '体检者姓名',
    id_number VARCHAR(255) NOT NULL COMMENT '身份证号',
    phone VARCHAR(255) DEFAULT NULL COMMENT '手机号',
    package_id BIGINT NOT NULL COMMENT '体检套餐ID',
    register_doctor_id BIGINT NULL COMMENT '登记医生ID（HR导入时为空）',
    register_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/IN_PROGRESS/LOCKED/PUBLISHED',
    locked_time DATETIME DEFAULT NULL COMMENT '锁定时间',
    source VARCHAR(20) DEFAULT 'INDIVIDUAL' COMMENT '来源: INDIVIDUAL / GROUP',
    CONSTRAINT fk_exam_package FOREIGN KEY (package_id) REFERENCES check_package(package_id),
    CONSTRAINT fk_exam_doctor  FOREIGN KEY (register_doctor_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检记录表';

CREATE TABLE IF NOT EXISTS physical_data (
    data_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    exam_id BIGINT NOT NULL COMMENT '体检记录ID',
    item_id BIGINT NOT NULL COMMENT '项目ID',
    item_value  VARCHAR(200) NOT NULL COMMENT '检查结果值',
    unit VARCHAR(20) DEFAULT NULL COMMENT '单位',
    record_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
    CONSTRAINT fk_pd_exam FOREIGN KEY (exam_id) REFERENCES examination(exam_id),
    CONSTRAINT fk_pd_item FOREIGN KEY (item_id) REFERENCES check_item(item_id),
    UNIQUE KEY uk_exam_item (exam_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检数据表';

CREATE TABLE IF NOT EXISTS final_report (
    report_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    exam_id BIGINT NOT NULL UNIQUE COMMENT '体检记录ID',
    doctor_id BIGINT NOT NULL COMMENT '发布医生ID',
    conclusion TEXT NOT NULL COMMENT '体检结论',
    advice TEXT NOT NULL COMMENT '健康建议',
    doctor_sign  VARCHAR(50)  NOT NULL COMMENT '医生签名',
    publish_time DATETIME DEFAULT NULL COMMENT '发布时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_report_exam   FOREIGN KEY (exam_id)   REFERENCES examination(exam_id),
    CONSTRAINT fk_report_doctor FOREIGN KEY (doctor_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='最终报告表';

CREATE TABLE IF NOT EXISTS daily_capacity_config (
    config_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    capacity_date DATE UNIQUE NOT NULL COMMENT '配置日期',
    total_capacity INT NOT NULL DEFAULT 100 COMMENT '当日总名额',
    morning_capacity INT NOT NULL DEFAULT 50 COMMENT '上午总名额',
    afternoon_capacity INT NOT NULL DEFAULT 50 COMMENT '下午总名额',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日名额配置表';

CREATE TABLE IF NOT EXISTS doctor_capacity_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    capacity_date DATE NOT NULL COMMENT '配置日期',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    morning_capacity INT NOT NULL DEFAULT 0 COMMENT '医生上午名额',
    afternoon_capacity  INT NOT NULL DEFAULT 0 COMMENT '医生下午名额',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_date_doctor (capacity_date, doctor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生时段名额配置表';

CREATE TABLE IF NOT EXISTS ai_suggestion_cache (
    cache_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL COMMENT '体检记录ID',
    data_hash VARCHAR(32) NOT NULL COMMENT '体检数据MD5哈希',
    ai_conclusion TEXT COMMENT 'AI生成的结论',
    ai_advice TEXT COMMENT 'AI生成的建议',
    model_type VARCHAR(20) DEFAULT 'RAG' COMMENT '模型类型：RAG/OLLAMA',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    UNIQUE KEY uk_exam_hash (exam_id, data_hash),
    INDEX idx_expires (expires_at),
    CONSTRAINT fk_cache_exam FOREIGN KEY (exam_id) REFERENCES examination(exam_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI建议缓存表';

INSERT INTO sys_user (username, password, real_name, role, is_active) VALUES
    ('lqx', '123', '系统管理员', 'ADMIN',  1),
    ('d1', '123456', '张医生', 'DOCTOR', 1),
    ('d2', '123456', '李医生', 'DOCTOR', 1),
    ('d3', '123456', '郝医生', 'DOCTOR', 1),
    ('d4', '123456', '孬医生', 'DOCTOR', 0),
    ('h1', '123456789', '王人事', 'HR', 1),
    ('h2', '123456789', '叶人事', 'HR', 0);

INSERT INTO check_item (item_code, item_name, unit, ref_range, category, is_active) VALUES
    ('XCGC',  '血红蛋白', 'g/L', '110-160', '血液检查', 1),
    ('XCGZ',  '白细胞计数', '10^9/L', '3.5-9.5', '血液检查', 1),
    ('XCNX',  '血小板计数', '10^9/L', '125-350', '血液检查', 1),
    ('NCGZ',  '谷丙转氨酶', 'U/L', '0-40', '肝功能', 1),
    ('NCGG',  '谷草转氨酶', 'U/L', '0-40', '肝功能', 1),
    ('NCDB',  '总胆红素', 'μmol/L', '3.4-17.1', '肝功能', 1),
    ('SXZZ',  '肌酐', 'μmol/L', '44-133', '肾功能', 1),
    ('SXXZ',  '尿素氮', 'mmol/L', '2.6-7.5', '肾功能', 1),
    ('XZDC',  '总胆固醇', 'mmol/L', '2.8-5.7', '血脂', 1),
    ('XZSG',  '甘油三酯', 'mmol/L', '0.56-1.7', '血脂', 1),
    ('XTKT',  '空腹血糖', 'mmol/L', '3.9-6.1', '血糖', 1),
    ('XY',    '血压', 'mmHg',   '90-140/60-90',  '一般检查', 1),
    ('SGTZ',  '身高体重', 'cm/kg',  '', '一般检查', 1),
    ('XDT',   '心电图', '', '', '辅助检查', 1),
    ('FBBQ',  '腹部B超', '', '', '辅助检查', 1),
    ('XWXG',  '胸部X光', '', '', '辅助检查', 1);

INSERT INTO check_package (package_name, price, description, is_active) VALUES
    ('基础体检套餐', 299.00, '涵盖血常规、肝肾功能、心电图等基础检查项目', 1),
    ('标准体检套餐', 599.00, '在基础套餐上增加血脂、血糖、腹部B超等项目', 1),
    ('精英体检套餐', 999.00, '全面检查，包含胸部X光及所有标准项目', 1);

INSERT INTO package_item (package_id, item_id) VALUES
    (1, 12),
    (1, 13),
    (1, 1),
    (1, 2),
    (1, 3),
    (1, 4),
    (1, 14);

INSERT INTO package_item (package_id, item_id) VALUES
    (2, 12),
    (2, 13),
    (2, 1),
    (2, 2),
    (2, 3),
    (2, 4),
    (2, 14),
    (2, 5),
    (2, 6),
    (2, 7),
    (2, 9),
    (2, 11);

INSERT INTO package_item (package_id, item_id) VALUES
    (3, 12),
    (3, 13),
    (3, 1),
    (3, 2),
    (3, 3),
    (3, 4),
    (3, 14),
    (3, 5),
    (3, 6),
    (3, 7),
    (3, 9),
    (3, 11),
    (3, 8),
    (3, 10),
    (3, 15),
    (3, 16);

INSERT INTO examination (real_name, id_number, phone, package_id, register_doctor_id, register_time, status, locked_time, source) VALUES
    ('赵小花', '110101199503072345', '13800001001', 1, 2, '2026-03-30 09:15:00', 'PENDING', NULL, 'INDIVIDUAL'),
    ('钱大壮', '320102198806154567', '13800001002', 2, 2, '2026-03-30 09:30:00', 'PENDING', NULL, 'INDIVIDUAL'),
    ('孙明月', '440103199209203456', '13800001003', 1, 2, '2026-03-30 09:45:00', 'IN_PROGRESS', NULL, 'INDIVIDUAL'),
    ('李建国', '510104199108084567', '13800001004', 2, 2, '2026-03-30 10:00:00', 'IN_PROGRESS', NULL, 'INDIVIDUAL'),
    ('周小燕', '330106198912125678', '13800001005', 1, 2, '2026-03-30 10:15:00', 'LOCKED', '2026-03-30 11:20:00','INDIVIDUAL'),
    ('吴晓峰', '350102199507066789', '13800001006', 3, 2, '2026-03-30 10:30:00', 'PENDING', NULL, 'INDIVIDUAL'),
    ('郑美玲', '210103198803157890', '13900002001', 3, 3, '2026-03-30 14:10:00', 'PENDING', NULL, 'INDIVIDUAL'),
    ('冯志强', '420104199604189901', '13900002002', 1, 3, '2026-03-30 14:25:00', 'PENDING', NULL, 'INDIVIDUAL'),
    ('陈思远', '610103199209209012', '13900002003', 2, 3, '2026-03-30 14:40:00', 'IN_PROGRESS', NULL, 'INDIVIDUAL'),
    ('楚云飞', '500103199311211234', '13900002004', 3, 3, '2026-03-30 14:55:00', 'IN_PROGRESS', NULL, 'INDIVIDUAL'),
    ('卫小宝', '130103199705253456', '13900002005', 2, 3, '2026-03-30 15:10:00', 'LOCKED', '2026-03-30 16:30:00','INDIVIDUAL'),
    ('蒋一鸣', '340104199001014567', '13900002006', 1, 3, '2026-03-30 15:25:00', 'IN_PROGRESS', NULL, 'INDIVIDUAL');

INSERT INTO physical_data (exam_id, item_id, item_value, unit, record_time) VALUES
    (3, 12, '118',   'mmHg',  '2026-03-30 10:00:00'),
    (3, 13, '165/55', 'cm/kg', '2026-03-30 10:01:00'),
    (3, 1,  '138',   'g/L',   '2026-03-30 10:02:00'),
    (3, 2,  '6.8',   '10^9/L','2026-03-30 10:03:00');

INSERT INTO physical_data (exam_id, item_id, item_value, unit, record_time) VALUES
    (4, 12, '132',   'mmHg',  '2026-03-30 10:20:00'),
    (4, 13, '178/82','cm/kg', '2026-03-30 10:21:00'),
    (4, 1,  '152',   'g/L',   '2026-03-30 10:22:00'),
    (4, 2,  '7.2',   '10^9/L','2026-03-30 10:23:00'),
    (4, 3,  '210',   '10^9/L','2026-03-30 10:24:00'),
    (4, 4,  '55',    'U/L',   '2026-03-30 10:25:00');

INSERT INTO physical_data (exam_id, item_id, item_value, unit, record_time) VALUES
    (5, 12, '115',   'mmHg',  '2026-03-30 10:30:00'),
    (5, 13, '162/50','cm/kg', '2026-03-30 10:31:00'),
    (5, 1,  '128',   'g/L',   '2026-03-30 10:32:00'),
    (5, 2,  '5.6',   '10^9/L','2026-03-30 10:33:00'),
    (5, 3,  '198',   '10^9/L','2026-03-30 10:34:00'),
    (5, 4,  '22',    'U/L',   '2026-03-30 10:35:00'),
    (5, 14, '窦性心律，正常心电图', '', '2026-03-30 10:36:00');

INSERT INTO physical_data (exam_id, item_id, item_value, unit, record_time) VALUES
    (8, 12, '125',   'mmHg',  '2026-03-30 15:00:00'),
    (8, 13, '170/68','cm/kg', '2026-03-30 15:01:00'),
    (8, 1,  '145',   'g/L',   '2026-03-30 15:02:00'),
    (8, 2,  '6.1',   '10^9/L','2026-03-30 15:03:00'),
    (8, 3,  '260',   '10^9/L','2026-03-30 15:04:00');

INSERT INTO physical_data (exam_id, item_id, item_value, unit, record_time) VALUES
    (9, 12, '142',   'mmHg',  '2026-03-30 15:20:00'),
    (9, 13, '175/90','cm/kg', '2026-03-30 15:21:00'),
    (9, 1,  '155',   'g/L',   '2026-03-30 15:22:00'),
    (9, 2,  '8.9',   '10^9/L','2026-03-30 15:23:00'),
    (9, 3,  '310',   '10^9/L','2026-03-30 15:24:00'),
    (9, 4,  '68',    'U/L',   '2026-03-30 15:25:00'),
    (9, 5,  '52',    'U/L',   '2026-03-30 15:26:00'),
    (9, 9,  '6.8',   'mmol/L','2026-03-30 15:27:00');

INSERT INTO physical_data (exam_id, item_id, item_value, unit, record_time) VALUES
    (10, 12, '110',   'mmHg',  '2026-03-30 15:40:00'),
    (10, 13, '158/48','cm/kg', '2026-03-30 15:41:00'),
    (10, 1,  '132',   'g/L',   '2026-03-30 15:42:00'),
    (10, 2,  '5.9',   '10^9/L','2026-03-30 15:43:00'),
    (10, 3,  '225',   '10^9/L','2026-03-30 15:44:00'),
    (10, 4,  '18',    'U/L',   '2026-03-30 15:45:00'),
    (10, 14, '窦性心律，正常心电图', '', '2026-03-30 15:46:00'),
    (10, 5,  '21',    'U/L',   '2026-03-30 15:47:00'),
    (10, 6,  '8.5',   'μmol/L','2026-03-30 15:48:00'),
    (10, 7,  '76',    'μmol/L','2026-03-30 15:49:00'),
    (10, 9,  '4.2',   'mmol/L','2026-03-30 15:50:00'),
    (10, 11, '5.0',   'mmol/L','2026-03-30 15:51:00');

INSERT INTO physical_data (exam_id, item_id, item_value, unit, record_time) VALUES
    (12, 12, '128',   'mmHg',  '2026-03-30 15:55:00'),
    (12, 13, '173/70','cm/kg', '2026-03-30 15:56:00');

UPDATE sys_user SET password = '$2a$10$5rmU39kjkXwnX2q3SYrka./NFVDVnCFB6W7pFjIY0uqWjlDzOSESG' WHERE username = 'lqx';
UPDATE sys_user SET password = '$2a$10$RxANqBHe07U.9iGQ6slJYu15Q00IxHAdk5DP.C6zHSvmxKi.dLery' WHERE username = 'd1';
UPDATE sys_user SET password = '$2a$10$p4HmnEtQiUf47UBoifQXCOkyUYGHeOTB.oBts/TSkl.MCnzeyAIHO' WHERE username = 'd2';
UPDATE sys_user SET password = '$2a$10$IrqWKbkMT6v2X7ycN5m6XuIXRFqgufafd1zUPXMvNDLnmML.rOvC.' WHERE username = 'd3';
UPDATE sys_user SET password = '$2a$10$UmHspWDn/zYK7/WcLhrABOeZ2WiojWjrSsZQaCh65XJ2Ik9RhCGUO' WHERE username = 'd4';
UPDATE sys_user SET password = '$2a$10$6oCeg3YTVJcHXSnvEuxMXOt9EoxCaNz1HwwZszy2RNBOjRqXtO30W' WHERE username = 'h1';
UPDATE sys_user SET password = '$2a$10$n6j2pJMeGCb5fJ76ogM/OOm34A0eNY85yGmA1RmbRva0SkfMLEzE.' WHERE username = 'h2';