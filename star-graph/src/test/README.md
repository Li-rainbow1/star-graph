# 单元测试说明

## 测试覆盖范围

### Text2ImageServiceImplTest

核心业务逻辑测试，覆盖以下场景：

#### 1. 取消任务测试（cancelTask）
- ✅ **成功场景**：正常取消任务并归还积分
- ✅ **任务ID为空**：参数校验失败
- ✅ **获取锁失败**：并发控制测试
- ✅ **任务已开始**：不允许取消已执行的任务
- ✅ **无权限操作**：只能取消自己的任务

#### 2. 插队测试（increasePriority）
- ✅ **成功场景**：成功提升优先级并扣费
- ✅ **已是第一名**：无需插队
- ✅ **Redis失败**：提升优先级失败处理

#### 3. 历史列表测试（getUserImageList）
- ✅ **成功场景**：正常分页查询
- ✅ **参数校验**：分页参数自动修正

## 测试技术栈

- **JUnit 5**：测试框架
- **Mockito**：Mock框架
- **MockedStatic**：静态方法Mock（UserUtils）

## 运行测试

### Maven命令
```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=Text2ImageServiceImplTest

# 运行单个测试方法
mvn test -Dtest=Text2ImageServiceImplTest#testCancelTask_Success
```

### IDE运行
1. 右键点击测试类或方法
2. 选择 "Run as JUnit Test"

## 依赖配置

确保 pom.xml 包含以下依赖：

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.9.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.3.1</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito JUnit Jupiter -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.3.1</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 测试覆盖率

当前测试覆盖核心业务逻辑的主要场景：
- **正常流程**：✅
- **异常场景**：✅
- **边界条件**：✅
- **并发控制**：✅
- **权限验证**：✅

## 注意事项

1. **MockedStatic使用**：`UserUtils.getUser()` 使用了静态方法Mock，需要在try-with-resources块中使用
2. **分布式锁**：通过Mock `StringRedisTemplate` 模拟锁的获取和释放
3. **类型警告**：存在一些类型安全警告，但不影响测试运行

## 未来扩展

可以进一步添加：
- 并发压力测试
- 集成测试（真实Redis环境）
- 性能基准测试
- 覆盖率报告生成
