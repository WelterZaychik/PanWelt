package asia.welter.component;

import asia.welter.entity.constants.Constants;
import asia.welter.entity.dto.SysSettingDto;
import asia.welter.entity.dto.UserSpaceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("RedisComponent")
public class RedisComponent {

    @Autowired
    private RedisUtils redisUtils;

    public SysSettingDto getSysSettingDto() {
        SysSettingDto sysSettingDto = (SysSettingDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingDto == null) {
            sysSettingDto = new SysSettingDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingDto);
        }
        return sysSettingDto;
    }

    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE+userId, userSpaceDto,Constants.REDIS_KEY_EXPIRES_DAY);

    }

    public UserSpaceDto getUserSpaceUse(String userId) {
        UserSpaceDto userSpaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (userSpaceDto == null) {
            UserSpaceDto spaceDto = new UserSpaceDto();

            //TODO 查询当前用户已经使用空间总和
            spaceDto.setUserSpace(0L);
            spaceDto.setTotalSpace(getSysSettingDto().getUserInitUseSpace()*Constants.MB);
            saveUserSpaceUse(userId, spaceDto);
        }
        return userSpaceDto;
    }
}
