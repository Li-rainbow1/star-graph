package cn.itcast.star.graph;

import cn.hutool.crypto.digest.BCrypt;
import cn.itcast.star.graph.core.mapper.UserMapper;
import cn.itcast.star.graph.core.pojo.User;
import cn.itcast.star.graph.core.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
public class UserTest {

    @Autowired
    UserMapper userMapper;
    @Autowired
    UserService userService;

    @Test
    public void testAddUser(){
        User user = new User();
        user.setAvatar("https://img1.baidu.com/it/u=1090452517,2487311686&fm=253&app=120&size=w931&n=0&f=JPEG&fmt=auto?sec=1729098000&t=bde85038085ca68a7e26aef57b0b6b2b");
        user.setUsername("admin");
        user.setPassword(BCrypt.hashpw("admin"));
        user.setMobile("1521030727");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
    }


    @Test
    public void addUser() {
        User user = new User();
        user.setId(1L);
        user.setMobile("15210307233");
        user.setPassword(BCrypt.hashpw("admin"));
        user.setUsername("admin");
        user.setStatus(1);
        user.setGender(1);
        user.setEmail("admin@126.com");
        user.setNickname("Admin");
        user.setVipLevel(1);
        userService.save(user);
    }

    @Test
    public void addUser2() {
        User user = new User();
        user.setId(2L);
        user.setMobile("15210307231");
        user.setPassword(BCrypt.hashpw("miukoo"));
        user.setUsername("miukoo");
        user.setStatus(1);
        user.setGender(1);
        user.setEmail("miukoo@126.com");
        user.setNickname("Miukoo");
        user.setVipLevel(1);
        userService.save(user);
    }
}
