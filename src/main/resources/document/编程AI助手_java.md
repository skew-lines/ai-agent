##  一、Java 基础

### 1.1 基本数据类型

| 类型    | 大小 | 默认值   | 示例                 |
| ------- | ---- | -------- | -------------------- |
| byte    | 1B   | 0        | byte b = 1;          |
| int     | 4B   | 0        | int i = 10;          |
| long    | 8B   | 0L       | long l = 100L;       |
| float   | 4B   | 0.0f     | float f = 1.0f;      |
| double  | 8B   | 0.0      | double d = 2.0;      |
| char    | 2B   | '\u0000' | char c = 'a';        |
| boolean | 1B   | false    | boolean flag = true; |

------

### 1.2 引用类型

- 类（Class）
- 接口（Interface）
- 数组（Array）
- 枚举（Enum）

------

## 🧠 二、面向对象编程（OOP）

### 2.1 三大特性

- 封装（Encapsulation）
- 继承（Inheritance）
- 多态（Polymorphism）

------

### 2.2 示例代码

```
class Animal {
    public void speak() {
        System.out.println("Animal speaking");
    }
}

class Dog extends Animal {
    @Override
    public void speak() {
        System.out.println("Dog barking");
    }
}

public class Main {
    public static void main(String[] args) {
        Animal a = new Dog();
        a.speak(); // 多态
    }
}
```

------

## ⚙️ 三、集合框架（Collections）

### 3.1 常用集合

| 类型       | 结构     | 特点           |
| ---------- | -------- | -------------- |
| ArrayList  | 动态数组 | 查询快，增删慢 |
| LinkedList | 链表     | 增删快         |
| HashMap    | 哈希表   | 无序，O(1)     |
| TreeMap    | 红黑树   | 有序           |
| HashSet    | 去重集合 | 基于 HashMap   |

------

### 3.2 HashMap 原理

- 数组 + 链表 + 红黑树（JDK8）
- hash 冲突 → 链表 → 树化（>=8）

```
Map<String, Integer> map = new HashMap<>();
map.put("a", 1);
map.get("a");
```

------

## 🚀 四、并发编程（JUC）

### 4.1 线程创建方式

```
// 方式1：继承 Thread
class MyThread extends Thread {
    public void run() {
        System.out.println("Thread running");
    }
}

// 方式2：实现 Runnable
Runnable r = () -> System.out.println("Runnable running");
new Thread(r).start();
```

------

### 4.2 常见并发工具

- `synchronized`
- `ReentrantLock`
- `CountDownLatch`
- `ThreadLocal`
- `CAS（Compare And Swap）`

------

### 4.3 线程池

```
ExecutorService pool = Executors.newFixedThreadPool(5);
pool.execute(() -> System.out.println("task"));
pool.shutdown();
```

------

## 🧱 五、JVM 基础

### 5.1 内存结构

- 堆（Heap）
- 栈（Stack）
- 方法区（Method Area）
- 程序计数器

------

### 5.2 垃圾回收（GC）

- 标记清除（Mark-Sweep）
- 复制算法（Copying）
- 标记整理（Mark-Compact）

------

## 🗄️ 六、常见面试知识点

### 6.1 equals vs ==

- `==`：比较地址
- `equals`：比较内容

------

### 6.2 String 不可变性

```
String s = "abc";
s = s + "d"; // 产生新对象
```

原因：

- 安全性
- 线程安全
- 字符串常量池优化

------

### 6.3 HashMap 扩容

- 默认容量 16
- 负载因子 0.75
- 扩容为原来的 2 倍

------

## 🧩 七、设计模式（常见）

### 7.1 单例模式

```
class Singleton {
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

------

## 📦 八、Spring 核心概念

### 8.1 IOC

- 控制反转
- Bean 生命周期由容器管理

------

### 8.2 AOP

- 面向切面编程
- 常用于：
  - 日志
  - 事务
  - 权限控制

------

## 🔍 九、常见问题速查（FAQ）

### Q1：为什么 HashMap 线程不安全？

- 多线程扩容会导致链表成环

------

### Q2：什么是死锁？

- 多线程互相等待资源

------

### Q3：什么是 CAS？

- 无锁算法
- 核心：比较并交换

------

## 🧾 十、总结（适合RAG检索）

**关键词索引：**

- Java 基础
- OOP
- HashMap 原理
- 并发编程
- JVM
- GC
- 设计模式
- Spring