-- 修复 Spring SQL init 在 Windows 下以 GBK 读取 UTF-8 脚本导致的中文乱码（幂等 UPDATE）
USE storage;

SET NAMES utf8mb4;

UPDATE sys_user SET display_name = '系统管理员' WHERE username = 'admin';

UPDATE sys_role SET name = '系统管理员' WHERE code = 'ADMIN';
UPDATE sys_role SET name = '普通用户' WHERE code = 'USER';

UPDATE sys_menu SET name = '物料台账' WHERE id = 1;
UPDATE sys_menu SET name = '物料台账写' WHERE id = 2;
UPDATE sys_menu SET name = '文件上传' WHERE id = 3;
UPDATE sys_menu SET name = '资源管理' WHERE id = 100;
UPDATE sys_menu SET name = '仓库管理' WHERE id = 110;
UPDATE sys_menu SET name = '物料台账' WHERE id = 111;
UPDATE sys_menu SET name = '物料出入库' WHERE id = 112;
UPDATE sys_menu SET name = '安全库存管理' WHERE id = 113;
UPDATE sys_menu SET name = '系统管理' WHERE id = 200;
UPDATE sys_menu SET name = '用户管理' WHERE id = 201;
UPDATE sys_menu SET name = '角色管理' WHERE id = 202;
UPDATE sys_menu SET name = '菜单管理' WHERE id = 203;
UPDATE sys_menu SET name = '客户管理' WHERE id = 204;
UPDATE sys_menu SET name = '用户写' WHERE id = 214;
UPDATE sys_menu SET name = '角色写' WHERE id = 224;
UPDATE sys_menu SET name = '菜单写' WHERE id = 234;
UPDATE sys_menu SET name = '客户写' WHERE id = 244;
UPDATE sys_menu SET name = '个人中心' WHERE id = 10;
UPDATE sys_menu SET name = '项目管理中心' WHERE id = 20;
UPDATE sys_menu SET name = '新建项目' WHERE id = 21;
UPDATE sys_menu SET name = '项目集' WHERE id = 22;
UPDATE sys_menu SET name = '采购管理' WHERE id = 120;
UPDATE sys_menu SET name = '采购管理' WHERE id = 121;
UPDATE sys_menu SET name = '新增采购需求' WHERE id = 122;
UPDATE sys_menu SET name = '我的采购需求' WHERE id = 123;
UPDATE sys_menu SET name = '配置管理' WHERE id = 114;
UPDATE sys_menu SET name = 'Bin位管理' WHERE id = 115;
UPDATE sys_menu SET name = '物料清单管理' WHERE id = 116;
UPDATE sys_menu SET name = '设计指引' WHERE id = 150;
UPDATE sys_menu SET name = '技能中心' WHERE id = 160;
UPDATE sys_menu SET name = '技能矩阵' WHERE id = 161;
UPDATE sys_menu SET name = '人才画像' WHERE id = 162;
UPDATE sys_menu SET name = '人才培训计划' WHERE id = 163;
UPDATE sys_menu SET name = '经验库' WHERE id = 170;
UPDATE sys_menu SET name = '财务中心' WHERE id = 180;
UPDATE sys_menu SET name = '业务分析看板' WHERE id = 181;
UPDATE sys_menu SET name = '财务结算中心' WHERE id = 182;
UPDATE sys_menu SET name = '成本分析中心' WHERE id = 183;
UPDATE sys_menu SET name = 'Bin位写' WHERE id = 254;
UPDATE sys_menu SET name = '物料清单写' WHERE id = 255;
UPDATE sys_menu SET name = '项目中心读' WHERE id = 23;
