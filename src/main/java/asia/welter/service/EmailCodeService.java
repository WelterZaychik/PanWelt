package asia.welter.service;

import asia.welter.entity.po.EmailCode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Welt
* @description 针对表【email_code】的数据库操作Service
* @createDate 2025-08-11 11:15:54
*/
public interface EmailCodeService extends IService<EmailCode> {

    void sendEmailCode(String email,Integer type);

    void checkEmailCode(String email, String emailCode);
}
