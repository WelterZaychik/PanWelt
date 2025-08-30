package asia.welter.mapper;

import asia.welter.entity.po.Users;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author Welt
* @description 针对表【users】的数据库操作Mapper
* @createDate 2025-08-09 21:20:34
* @Entity asia.welter.entity.po.Users
*/
public interface UsersMapper extends BaseMapper<Users> {

    Integer updateUserSpace(@Param("userId") String userId, @Param("useSpace") Long useSpace, @Param("totalSpace") Long totalSpace);
}




