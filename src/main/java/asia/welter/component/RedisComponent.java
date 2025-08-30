package asia.welter.component;

import asia.welter.entity.constants.Constants;
import asia.welter.entity.dto.DownloadFileDto;
import asia.welter.entity.dto.SysSettingDto;
import asia.welter.entity.dto.UserSpaceDto;
import asia.welter.entity.po.FileInfo;
import asia.welter.entity.po.Users;
import asia.welter.mapper.FileInfoMapper;
import asia.welter.mapper.UsersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("RedisComponent")
public class RedisComponent {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private FileInfoMapper fileInfoMapper;
    @Autowired
    private UsersMapper usersMapper;

    /**
     * 获取系统设置
     * @return
     */
    public SysSettingDto getSysSettingDto() {
        SysSettingDto sysSettingDto = (SysSettingDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingDto == null) {
            sysSettingDto = new SysSettingDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingDto);
        }
        return sysSettingDto;
    }


    /**
     * 保存用户使用空间
     * @param userId
     * @param userSpaceDto
     */
    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE+userId, userSpaceDto,Constants.REDIS_KEY_EXPIRES_DAY);

    }

    /**
     * 获取用户使用空间
     * @param userId
     * @return
     */
    public UserSpaceDto getUserSpaceUse(String userId) {
        UserSpaceDto userSpaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (userSpaceDto == null) {
            UserSpaceDto spaceDto = new UserSpaceDto();

            Users tempUser = usersMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getUserId, userId));

            spaceDto.setUserSpace(tempUser.getUseSpace());
            spaceDto.setTotalSpace(getSysSettingDto().getUserInitUseSpace()*Constants.MB);
            saveUserSpaceUse(userId, spaceDto);
        }
        return userSpaceDto;
    }

    public UserSpaceDto resetUserSpaceUse(String userId) {
        UserSpaceDto spaceDto = new UserSpaceDto();
        Long userSpace = fileInfoMapper.selectUserUsedSpace(userId);
        spaceDto.setUserSpace(userSpace);

        Users users = usersMapper.selectById(userId);
//        UserInfo userInfo = this.userInfoMapper.selectByUserId(userId);
        spaceDto.setTotalSpace(users.getTotalSpace());
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
        return spaceDto;
    }


    public void saveDownloadCode(String code, DownloadFileDto downloadFileDto) {
        redisUtils.setex(Constants.REDIS_KEY_DOWNLOAD + code, downloadFileDto, Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
    }

    public DownloadFileDto getDownloadCode(String code) {
        return (DownloadFileDto) redisUtils.get(Constants.REDIS_KEY_DOWNLOAD + code);
    }


    //保存文件临时大小
    public void saveFileTempSize(String userId, String fileId, Long fileSize) {
        Long currentSize = getFileTempSize(userId, fileId);
        redisUtils.setex(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId, currentSize + fileSize, Constants.REDIS_KEY_EXPIRES_ONE_HOUR);
    }

    public Long getFileTempSize(String userId, String fileId) {
        Long currentSize = getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
        return currentSize;
    }

    private Long getFileSizeFromRedis(String key) {
        Object sizeObj = redisUtils.get(key);
        if (sizeObj == null) {
            return 0L;
        }
        if (sizeObj instanceof Integer) {
            return ((Integer) sizeObj).longValue();
        } else if (sizeObj instanceof Long) {
            return (Long) sizeObj;
        }

        return 0L;
    }



}
