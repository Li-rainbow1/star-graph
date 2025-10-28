package cn.itcast.star.graph.core.mapper;

import cn.itcast.star.graph.core.pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【sg_user(用户信息表)】的数据库操作Mapper
* @createDate 2025-10-19 21:06:12
* @Entity cn.itcast.star.graph.core.pojo.SgUser
*/
public interface UserMapper extends BaseMapper<User> {

}
