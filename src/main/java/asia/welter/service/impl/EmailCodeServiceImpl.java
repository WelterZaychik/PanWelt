package asia.welter.service.impl;

import asia.welter.entity.constants.Constants;
import asia.welter.entity.po.Users;
import asia.welter.exception.BusinessException;
import asia.welter.mapper.UsersMapper;
import asia.welter.utils.StringTools;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import asia.welter.entity.po.EmailCode;
import asia.welter.service.EmailCodeService;
import asia.welter.mapper.EmailCodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author Welt
* @description 针对表【email_code】的数据库操作Service实现
* @createDate 2025-08-11 11:15:54
*/
@Service
public class EmailCodeServiceImpl extends ServiceImpl<EmailCodeMapper, EmailCode>
    implements EmailCodeService{

    @Autowired
    private EmailCodeMapper emailCodeMapper;

    @Autowired
    private UsersMapper usersMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String email, Integer type) {
        if (type == Constants.ZERO){
            Users user = usersMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getEmail,email));
            if (user != null){
                throw new BusinessException("邮箱已存在");
            }
        }

        String code = StringTools.getRandomNumber(Constants.LENGTH_5);
        //TODO 发送验证码



        int updatedRows = emailCodeMapper.update(null, new LambdaUpdateWrapper<EmailCode>()
                        .eq(EmailCode::getEmail,email)
                        .eq(EmailCode::getStatus,Constants.ZERO)
                        .set(EmailCode::getStatus,Constants.ONE));

        EmailCode emailCode = new EmailCode();
        emailCode.setEmail(email);
        emailCode.setCode(code);
        emailCode.setStatus(Constants.ZERO);
        emailCodeMapper.insert(emailCode);
    }
}




