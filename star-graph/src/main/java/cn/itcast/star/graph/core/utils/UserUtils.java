package cn.itcast.star.graph.core.utils;

import cn.itcast.star.graph.core.pojo.User;

/**
 * 用户上下文工具类 - 使用ThreadLocal存储当前请求的用户信息
 */
public class UserUtils {
    // 存储当前线程的用户信息
    static ThreadLocal<User> local = new ThreadLocal<>();

    /**
     * 保存用户到ThreadLocal
     */
    public static void saveUser(User user){
        local.set(user);
    }

    /**
     * 获取当前用户
     */
    public static User getUser(){
        return local.get();
    }

    /**
     * 清除ThreadLocal，防止内存泄漏
     */
    public static void removeUser(){
        local.remove();
    }
}
