package asia.welter.service;



import asia.welter.entity.dto.SessionWebUserDto;
import asia.welter.entity.dto.UploadResultDto;
import asia.welter.entity.po.FileInfo;
import asia.welter.entity.query.FileInfoQuery;
import asia.welter.entity.vo.PaginationResultVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author Welt
* @description 针对表【file_info(文件信息)】的数据库操作Service
* @createDate 2025-08-23 15:44:13
*/
public interface FileInfoService extends IService<FileInfo> {

    PaginationResultVO findListByPage(FileInfoQuery page);

    UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    Long getUserUseSpace(String userId);

    FileInfo getFileInfoByFileIdAndUserId(String realFileId, String userId);

    List<FileInfo> findListByParam(FileInfoQuery fileInfoQuery);

    Integer findCountByParam(FileInfoQuery fileInfoQuery);
}
