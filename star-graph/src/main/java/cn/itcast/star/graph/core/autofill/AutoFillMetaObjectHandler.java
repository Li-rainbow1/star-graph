// 定义包路径
package cn.itcast.star.graph.core.autofill;

// 导入MyBatis Plus的元对象处理器接口，用于实现自动填充功能
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
// 导入MyBatis的元对象类，封装了实体对象的元数据信息
import org.apache.ibatis.reflection.MetaObject;
// 导入Spring的Component注解，将此类注册为Spring容器管理的Bean
import org.springframework.stereotype.Component;

// 导入Java 8的LocalDate类，表示日期（未使用，但保留导入）
import java.time.LocalDate;
// 导入Java 8的LocalDateTime类，表示日期时间，用于时间戳字段
import java.time.LocalDateTime;

/**
 * MyBatis Plus自动填充处理器
 * 
 * <p>自动填充实体类的创建时间和更新时间字段，无需手动设置
 * 
 * <p>工作原理：
 * <ul>
 *     <li>实体类字段使用@TableField(fill = FieldFill.INSERT)标注插入时自动填充</li>
 *     <li>实体类字段使用@TableField(fill = FieldFill.UPDATE)标注更新时自动填充</li>
 *     <li>MyBatis Plus在执行insert/update时自动调用对应的填充方法</li>
 * </ul>
 * 
 * <p>使用示例（实体类）：
 * <pre>
 * public class User {
 *     // 插入时自动填充
 *     &#64;TableField(fill = FieldFill.INSERT)
 *     private LocalDateTime createdTime;
 *     
 *     // 插入和更新时都自动填充
 *     &#64;TableField(fill = FieldFill.INSERT_UPDATE)
 *     private LocalDateTime updatedTime;
 * }
 * </pre>
 * 
 * <p>优点：
 * <ul>
 *     <li>统一管理时间字段的填充逻辑</li>
 *     <li>避免在业务代码中重复设置时间</li>
 *     <li>确保时间字段的一致性和准确性</li>
 *     <li>减少代码量，提高开发效率</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
// 标识这是一个Spring组件，会被自动扫描并注册到Spring容器
@Component
// 实现MetaObjectHandler接口，提供自动填充的具体实现
public class AutoFillMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入数据时的自动填充方法
     * 
     * <p>在执行INSERT操作时，MyBatis Plus会自动调用此方法，
     * 为标注了@TableField(fill = FieldFill.INSERT)的字段填充值
     * 
     * <p>填充字段说明：
     * <ul>
     *     <li>createdTime：创建时间（常用命名）</li>
     *     <li>createTime：创建时间（兼容不同命名习惯）</li>
     * </ul>
     * 
     * <p>填充时机：
     * <ul>
     *     <li>在SQL执行之前填充</li>
     *     <li>只在INSERT操作时触发</li>
     *     <li>字段值为null时才填充（strictInsertFill模式）</li>
     * </ul>
     * 
     * @param metaObject MyBatis的元对象，包含实体对象的元数据和属性信息
     */
    @Override  // 重写父接口的方法
    public void insertFill(MetaObject metaObject) {
        // 严格模式插入填充：为"createdTime"字段自动填充当前时间
        // 参数1：metaObject - 元对象，包含实体信息
        // 参数2："createdTime" - 要填充的字段名（对应实体类的属性名）
        // 参数3：LocalDateTime.class - 字段的类型
        // 参数4：LocalDateTime.now() - 填充的值，当前日期时间
        // strictInsertFill：严格模式，只有当字段值为null时才填充，避免覆盖已设置的值
        this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, LocalDateTime.now());
        
        // 严格模式插入填充：为"createTime"字段自动填充当前时间
        // 支持不同的命名习惯（createdTime vs createTime）
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新数据时的自动填充方法
     * 
     * <p>在执行UPDATE操作时，MyBatis Plus会自动调用此方法，
     * 为标注了@TableField(fill = FieldFill.UPDATE)或
     * @TableField(fill = FieldFill.INSERT_UPDATE)的字段填充值
     * 
     * <p>填充字段说明：
     * <ul>
     *     <li>updatedTime：更新时间，记录最后一次修改的时间</li>
     * </ul>
     * 
     * <p>填充时机：
     * <ul>
     *     <li>在SQL执行之前填充</li>
     *     <li>只在UPDATE操作时触发</li>
     *     <li>每次更新都会填充最新时间</li>
     * </ul>
     * 
     * <p>应用场景：
     * <ul>
     *     <li>审计跟踪：记录数据的最后修改时间</li>
     *     <li>缓存失效：根据更新时间判断缓存是否过期</li>
     *     <li>版本控制：结合乐观锁实现并发控制</li>
     * </ul>
     * 
     * @param metaObject MyBatis的元对象，包含实体对象的元数据和属性信息
     */
    @Override  // 重写父接口的方法
    public void updateFill(MetaObject metaObject) {
        // 严格模式更新填充：为"updatedTime"字段自动填充当前时间
        // 参数1：metaObject - 元对象，包含实体信息
        // 参数2："updatedTime" - 要填充的字段名（对应实体类的属性名）
        // 参数3：LocalDateTime.class - 字段的类型
        // 参数4：LocalDateTime.now() - 填充的值，当前日期时间
        // strictUpdateFill：严格模式更新填充，确保每次更新都填充最新时间
        this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
    }
}
