package asia.welter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import asia.welter.entity.po.Users;
import asia.welter.service.UsersService;
import asia.welter.mapper.UsersMapper;
import org.springframework.stereotype.Service;

/**
* @author Welt
* @description 针对表【users】的数据库操作Service实现
* @createDate 2025-08-09 21:20:34
*/
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users>
    implements UsersService{

}




