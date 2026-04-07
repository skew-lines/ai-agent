## 一、MySQL 基础

### 1.1 常见存储引擎

| 引擎   | 特点                         |
| ------ | ---------------------------- |
| InnoDB | 支持事务、行锁、MVCC（默认） |
| MyISAM | 不支持事务、表锁             |
| Memory | 内存存储，速度快             |

------

### 1.2 数据类型

| 类型   | 示例                |
| ------ | ------------------- |
| 整型   | INT, BIGINT         |
| 浮点型 | FLOAT, DOUBLE       |
| 字符串 | VARCHAR, TEXT       |
| 时间   | DATETIME, TIMESTAMP |

------

## ⚙️ 二、SQL 基础

### 2.1 基本 CRUD

```
-- 查询
SELECT * FROM user WHERE age > 18;

-- 插入
INSERT INTO user(name, age) VALUES('Tom', 20);

-- 更新
UPDATE user SET age = 21 WHERE id = 1;

-- 删除
DELETE FROM user WHERE id = 1;
```

------

### 2.2 JOIN 类型

| 类型       | 说明     |
| ---------- | -------- |
| INNER JOIN | 交集     |
| LEFT JOIN  | 左表全部 |
| RIGHT JOIN | 右表全部 |

```
SELECT u.name, o.order_id
FROM user u
LEFT JOIN orders o ON u.id = o.user_id;
```

------

## 🧠 三、索引（重点🔥）

### 3.1 索引类型

- 主键索引（Primary Key）
- 唯一索引（Unique）
- 普通索引（Index）
- 复合索引（联合索引）

------

### 3.2 B+树索引原理

- 数据存储在叶子节点
- 叶子节点通过链表连接（范围查询快）
- 非叶子节点只存索引

------

### 3.3 最左前缀原则

```
-- index(a, b, c)

WHERE a = 1       ✅
WHERE a = 1 AND b = 2 ✅
WHERE b = 2       ❌
```

------

### 3.4 覆盖索引

- 查询字段全部在索引中
- 不需要回表

```
SELECT name FROM user WHERE id = 1;
```

------

## 🚀 四、事务（Transaction）

### 4.1 ACID 特性

| 特性        | 说明   |
| ----------- | ------ |
| Atomicity   | 原子性 |
| Consistency | 一致性 |
| Isolation   | 隔离性 |
| Durability  | 持久性 |

------

### 4.2 隔离级别

| 级别                    | 问题                 |
| ----------------------- | -------------------- |
| READ UNCOMMITTED        | 脏读                 |
| READ COMMITTED          | 不可重复读           |
| REPEATABLE READ（默认） | 幻读（InnoDB已优化） |
| SERIALIZABLE            | 完全串行             |

------

### 4.3 MVCC（多版本并发控制）

- 基于 undo log
- 每行数据隐藏字段：
  - trx_id（事务ID）
  - roll_pointer（回滚指针）

------

## 🧱 五、锁机制

### 5.1 锁类型

- 行锁（InnoDB）
- 表锁
- 间隙锁（Gap Lock）
- 临键锁（Next-Key Lock）

------

### 5.2 死锁

```
事务A：锁1 → 等锁2
事务B：锁2 → 等锁1
```

解决：

- 超时机制
- 死锁检测

------

## 🗄️ 六、日志系统（重点🔥）

### 6.1 redo log（重做日志）

- 保证持久性
- 物理日志
- WAL（先写日志）

------

### 6.2 undo log（回滚日志）

- 用于回滚
- 支持 MVCC

------

### 6.3 binlog（二进制日志）

- 用于主从复制
- 逻辑日志

------

## 🔄 七、执行流程（SQL执行过程）

1. 客户端发送 SQL
2. 查询缓存（已废弃）
3. 解析器（语法分析）
4. 优化器（选择索引）
5. 执行器（调用存储引擎）

------

## 📊 八、性能优化

### 8.1 SQL 优化原则

- 避免 SELECT *
- 使用索引
- 减少子查询（用 JOIN）
- LIMIT 分页优化

------

### 8.2 EXPLAIN 分析

```
EXPLAIN SELECT * FROM user WHERE id = 1;
```

关键字段：

| 字段 | 说明                           |
| ---- | ------------------------------ |
| type | 访问类型（最好是 const / ref） |
| key  | 使用的索引                     |
| rows | 扫描行数                       |

------

### 8.3 分页优化

```
-- 慢
SELECT * FROM user LIMIT 100000, 10;

-- 优化
SELECT * FROM user WHERE id > 100000 LIMIT 10;
```

------

## 🔥 九、高频面试问题

### 9.1 为什么用 B+树不用红黑树？

- IO 次数更少
- 范围查询更快

------

### 9.2 InnoDB 为什么用聚簇索引？

- 主键索引存储整行数据
- 减少回表

------

### 9.3 什么是回表？

- 通过索引找到主键，再查数据

------

### 9.4 什么是覆盖索引？

- 索引直接返回数据

------

### 9.5 为什么 Hash 索引不适合 MySQL？

- 不支持范围查询
- 不支持排序

------

## 🧾 十、总结（RAG检索关键词）

**关键词：**

- MySQL 索引
- B+树
- 事务 ACID
- MVCC
- redo log / undo log / binlog
- 锁机制
- SQL 优化
- EXPLAIN