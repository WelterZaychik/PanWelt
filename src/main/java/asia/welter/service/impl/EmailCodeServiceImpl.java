package asia.welter.service.impl;

import asia.welter.component.RedisComponent;
import asia.welter.entity.config.AppConfig;
import asia.welter.entity.constants.Constants;
import asia.welter.entity.dto.SysSettingDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.internet.MimeMessage;
import java.util.Date;


/**
* @author Welt
* @description 针对表【email_code】的数据库操作Service实现
* @createDate 2025-08-11 11:15:54
*/
@Service
public class EmailCodeServiceImpl extends ServiceImpl<EmailCodeMapper, EmailCode>
    implements EmailCodeService{

    @Autowired
    private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);

    @Autowired
    private EmailCodeMapper emailCodeMapper;

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private RedisComponent redisComponent;


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
        //发送验证码

        sendMailCode(email, code);



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

    private void sendMailCode(String toEmail , String code){
       try {
           MimeMessage mimeMessage = mailSender.createMimeMessage();
           MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "utf-8");
           mimeMessageHelper.setFrom(appConfig.getSendUserName());
           mimeMessageHelper.setTo(toEmail);

           SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();

           mimeMessageHelper.setSubject(sysSettingDto.getRegisterMailTitle());
           mimeMessageHelper.setText(String.format(sysSettingDto.getRegisterEmailContent(), code));
           mimeMessageHelper.setSentDate(new Date());
           mailSender.send(mimeMessage);
       } catch (Exception e) {
           logger.error("邮件发送失败", e);
           throw new BusinessException("邮件发送失败");
       }
    }
}




