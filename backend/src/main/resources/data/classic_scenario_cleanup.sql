-- 清理无效经典推演场景（测试脏数据、0 电池等）
DELETE FROM scenarios
WHERE LOWER(TRIM(name)) = 'test'
   OR TRIM(name) = ''
   OR duration IS NULL OR duration <= 0
   OR JSON_EXTRACT(initial_conditions, '$.batteryCount') IS NULL
   OR CAST(JSON_EXTRACT(initial_conditions, '$.batteryCount') AS UNSIGNED) = 0;

-- 经典推演场景描述与真实案例关联说明
UPDATE scenarios SET description = '【L1 入门】基于深圳储能电站单模组热失控案例抽象，训练早期识别、断电冷却与灭火决策' WHERE id = '30000000-0000-0000-0000-000000000001';
UPDATE scenarios SET description = '【L2 进阶】基于南京储能热扩散事故抽象，训练模组隔离、区域消防与全站响应决策' WHERE id = '30000000-0000-0000-0000-000000000002';
UPDATE scenarios SET description = '【L3 高级】基于 Gateway 250MW 大型储能火灾案例抽象，训练全舱指挥、撤离与长期监护决策' WHERE id = '30000000-0000-0000-0000-000000000003';

-- 应急决策训练场景（trainingKind=emergency_case）仅用于「应急训练」模块，不在经典推演列表展示
