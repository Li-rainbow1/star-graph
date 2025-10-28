// 定义包路径
package cn.itcast.star.graph.core.config;

// 导入Redisson核心类，用于创建RedissonClient实例
import org.redisson.Redisson;
// 导入RedissonClient接口，提供Redis分布式数据结构和服务
import org.redisson.api.RedissonClient;
// 导入Redisson配置类，用于配置Redis连接信息
import org.redisson.config.Config;
// 导入Spring Boot的Redis配置属性类，从application.yml自动读取配置
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
// 导入Spring的Bean注解，用于声明Spring管理的Bean
import org.springframework.context.annotation.Bean;
// 导入Lettuce连接工厂，Lettuce是Spring Data Redis默认的Redis客户端
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
// 导入StringRedisTemplate，提供Redis字符串操作的模板类
import org.springframework.data.redis.core.StringRedisTemplate;
// 导入Spring的Component注解，标识这是一个Spring组件
import org.springframework.stereotype.Component;

/**
 * Redisson和Redis配置类
 * 
 * <p>配置Redis相关的客户端，包括：
 * <ul>
 *     <li>RedissonClient：用于分布式锁、队列等高级功能</li>
 *     <li>StringRedisTemplate：用于基础的Redis操作</li>
 * </ul>
 * 
 * <p>Redis在本项目中的应用场景：
 * <ul>
 *     <li>任务队列管理（使用ZSet实现优先级队列）</li>
 *     <li>分布式锁（使用RLock防止并发冲突）</li>
 *     <li>任务信息缓存（使用String存储JSON数据）</li>
 *     <li>信号量控制（限制ComfyUI并发任务数）</li>
 * </ul>
 * 
 * <p>两个客户端的区别：
 * <ul>
 *     <li>RedissonClient：功能强大，提供分布式锁、队列、信号量等高级功能</li>
 *     <li>StringRedisTemplate：轻量级，适合简单的键值对操作</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
@Component  // 标识这是一个Spring组件，自动扫描并注册到容器
public class RedissionConfig {
    /**
     * 配置Redisson客户端Bean
     * 
     * <p>Redisson是Redis的Java客户端，提供了丰富的分布式数据结构和服务
     * 
     * <p>本项目使用的Redisson功能：
     * <ul>
     *     <li>RScoredSortedSet：有序集合，实现任务优先级队列（score越小优先级越高）</li>
     *     <li>RLock：分布式锁，用于定时任务的分布式协调</li>
     *     <li>RSemaphore：信号量，控制ComfyUI的并发任务数（最多10个）</li>
     * </ul>
     * 
     * <p>连接模式：单机模式（Single Server Mode）
     * 
     * @param redisProperties Spring Boot自动注入的Redis配置属性，从application.yml读取
     * @return Redisson客户端实例，注册到Spring容器供其他组件使用
     */
    @Bean  // 将返回值注册为Spring容器中的Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        // 创建Redisson配置对象
        Config config = new Config();
        
        // 配置单机模式的Redis连接
        // useSingleServer()：使用单机模式（相对于集群模式、哨兵模式等）
        // setAddress()：设置Redis服务器地址，格式：redis://host:port
        // String.format()：拼接Redis地址，从redisProperties获取host和port
        // redisProperties.getHost()：获取Redis主机地址（如：192.168.100.129）
        // redisProperties.getPort()：获取Redis端口号（默认：6379）
        config.useSingleServer().setAddress(String.format("redis://%s:%s", redisProperties.getHost(), redisProperties.getPort()));
        
        // 使用配置创建并返回RedissonClient实例
        // Redisson.create()：根据配置创建客户端，建立到Redis的连接
        return Redisson.create(config);
    }

    /**
     * 配置StringRedisTemplate Bean
     * 
     * <p>StringRedisTemplate是Spring Data Redis提供的模板类，
     * 专门用于处理字符串类型的键值对，序列化器使用StringRedisSerializer
     * 
     * <p>本项目使用场景：
     * <ul>
     *     <li>存储任务详细信息：key=task:{tempId}，value=ComfyuiTask的JSON字符串</li>
     *     <li>实现分布式锁：使用SETNX命令和过期时间</li>
     *     <li>临时数据缓存：如用户会话信息、临时状态等</li>
     * </ul>
     * 
     * <p>与RedisTemplate的区别：
     * <ul>
     *     <li>StringRedisTemplate：键值都是String类型，适合存储JSON、文本</li>
     *     <li>RedisTemplate：可存储任意对象，需要配置序列化器（如JDK、JSON）</li>
     * </ul>
     * 
     * <p>底层客户端：Lettuce（Spring Boot默认Redis客户端，基于Netty，支持异步）
     * 
     * @param redisProperties Spring Boot自动注入的Redis配置属性
     * @return StringRedisTemplate实例，注册到Spring容器供其他组件使用
     */
    @Bean  // 将返回值注册为Spring容器中的Bean
    public StringRedisTemplate stringRedisTemplate(RedisProperties redisProperties){
        // 创建Lettuce连接工厂
        // LettuceConnectionFactory是Spring Data Redis的连接工厂实现
        // 参数1：Redis主机地址（如：192.168.100.129）
        // 参数2：Redis端口号（默认：6379）
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());
        
        // 启动连接工厂，建立到Redis服务器的连接
        // 必须调用start()方法，否则无法使用
        lettuceConnectionFactory.start();
        
        // 创建并返回StringRedisTemplate实例
        // 构造函数传入连接工厂，StringRedisTemplate会使用该工厂创建Redis连接
        // StringRedisTemplate默认使用StringRedisSerializer进行序列化
        return new StringRedisTemplate(lettuceConnectionFactory);
    }
}
