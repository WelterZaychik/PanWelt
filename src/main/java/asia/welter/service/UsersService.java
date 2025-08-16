package asia.welter.service;

import asia.welter.annotation.VerifyParam;
import asia.welter.entity.dto.SessionWebUserDto;
import asia.welter.entity.enums.VerifyRegexEnum;
import asia.welter.entity.po.Users;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Welt
* @description 针对表【users】的数据库操作Service
* @createDate 2025-08-09 21:20:34
*/
public interface UsersService extends IService<Users> {

    void register(String email, String nickName, String password, String emailCode);

    SessionWebUserDto login(String email, String password);
}
