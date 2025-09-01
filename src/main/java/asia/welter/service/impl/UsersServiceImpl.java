package asia.welter.service.impl;

import asia.welter.component.RedisComponent;
import asia.welter.entity.config.AppConfig;
import asia.welter.entity.constants.Constants;
import asia.welter.entity.dto.SessionWebUserDto;
import asia.welter.entity.dto.SysSettingDto;
import asia.welter.entity.dto.UserSpaceDto;
import asia.welter.entity.enums.UserStatusEnum;
import asia.welter.exception.BusinessException;
import asia.welter.service.EmailCodeService;
import asia.welter.service.FileInfoService;
import asia.welter.utils.StringTools;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import asia.welter.entity.po.Users;
import asia.welter.service.UsersService;
import asia.welter.mapper.UsersMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.Date;


/**
* @author Welt
* @description 针对表【users】的数据库操作Service实现
* @createDate 2025-08-09 21:20:34
*/
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users>
    implements UsersService{

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private AppConfig appConfig;


    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void register(String email, String nickName, String password, String emailCode) {
        Users tempUser = usersMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getEmail, email));
        if (tempUser != null) {
            throw new BusinessException("邮箱账号已存在");
        }

        tempUser = usersMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getNickName, nickName));
        if (tempUser != null) {
            throw new BusinessException("昵称已存在");
        }

        //校验邮箱验证码
        emailCodeService.checkEmailCode(email, emailCode);

        String userId = StringTools.getRandomNumber(Constants.LENGTH_10);
        tempUser = new Users();
        tempUser.setUserId(userId);
        tempUser.setNickName(nickName);
        tempUser.setEmail(email);
        tempUser.setPassword(StringTools.encodeByMD5(password));
        tempUser.setStatus(UserStatusEnum.ENABLE.getStatus());
        tempUser.setUseSpace(0L);

        SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();

        tempUser.setTotalSpace(sysSettingDto.getUserInitUseSpace() * Constants.MB);
        usersMapper.insert(tempUser);

    }

    @Override
    public SessionWebUserDto login(String email, String password) {

        Users users = usersMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getEmail, email));

        if (users == null || !users.getPassword().equals(password)) {
            throw new BusinessException("账号或者密码错误");
        }

        if (users.getStatus() == UserStatusEnum.DISABLE.getStatus()){
            throw new BusinessException("账号已禁用");
        }

        Users updateUser = new Users();
        updateUser.setLastLoginTime(LocalDateTime.now());
        usersMapper.updateById(updateUser);

        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setNickName(users.getNickName());
        sessionWebUserDto.setUserId(users.getUserId());
        if (ArrayUtils.contains(appConfig.getAdminEmails().split(","), email)) {
            sessionWebUserDto.setIsAdmin(true);
        }else{
            sessionWebUserDto.setIsAdmin(false);
        }

        //用户空间
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        userSpaceDto.setUseSpace(fileInfoService.getUserUseSpace(users.getUserId()));
        userSpaceDto.setTotalSpace(users.getTotalSpace());
        redisComponent.saveUserSpaceUse(users.getUserId(), userSpaceDto);
        return sessionWebUserDto;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void resetPwd(String email, String newPwd, String emailCode) {
        Users tmpUser = usersMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getEmail, email));
        if (tmpUser == null) {
            throw new BusinessException("邮箱账号不存在");
        }
        emailCodeService.checkEmailCode(email, emailCode);

        tmpUser.setPassword(StringTools.encodeByMD5(newPwd));
        usersMapper.updateById(tmpUser);
    }
}




