package asia.welter.mapper;



import asia.welter.entity.po.FileInfo;
import asia.welter.entity.query.FileInfoQuery;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author Welt
* @description 针对表【file_info(文件信息)】的数据库操作Mapper
* @createDate 2025-08-23 15:44:13
* @Entity asia.welter.entity/po.FileInfo
*/
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    @Select("SELECT IFNULL(SUM(file_size), 0) FROM file_info WHERE user_id = #{userId}")
    Long selectUserUsedSpace(@Param("userId") String userId);



    void updateFileStatusWithOldStatus(String fileId, String userId, FileInfo updateInfo, Integer status);

    FileInfo selectByFileIdAndUserId(String realFileId, String userId);

    List<FileInfo> selectFileList(FileInfoQuery query);
}




