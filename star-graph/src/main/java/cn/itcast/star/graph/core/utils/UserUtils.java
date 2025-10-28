package cn.itcast.star.graph.core.utils;


import cn.itcast.star.graph.core.pojo.User;

/**
 * 用户上下文工具类
 * 
 * <p>使用ThreadLocal存储当前请求的用户信息，方便在同一请求的各个层级中获取用户
 * 
 * <p>主要功能：
 * <ul>
 *     <li>在拦截器中保存用户信息</li>
 *     <li>在Service、Controller中获取当前用户</li>
 *     <li>请求结束后清理ThreadLocal，防止内存泄漏</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
public class UserUtils {
    /** 存储当前线程的用户信息 */
    static ThreadLocal<User> local = new ThreadLocal<>();

    /**
     * 保存用户信息到当前线程
     * 
     * @param user 用户对象
     */
    public static void saveUser(User user){
        local.set(user);
    }

    /**
     * 获取当前线程的用户信息
     * 
     * @return 用户对象
     */
    public static User getUser(){
        return local.get();
    }

    /**
     * 清除当前线程的用户信息
     * 
     * <p>必须在请求结束后调用，防止内存泄漏
     */
    public static void removeUser(){
        local.remove();
    }
}
