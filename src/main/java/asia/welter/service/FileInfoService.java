package asia.welter.service;



import asia.welter.entity.po.FileInfo;
import asia.welter.entity.query.FileInfoQuery;
import asia.welter.entity.vo.PaginationResultVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Welt
* @description 针对表【file_info(文件信息)】的数据库操作Service
* @createDate 2025-08-23 15:44:13
*/
public interface FileInfoService extends IService<FileInfo> {

    PaginationResultVO findListByPage(FileInfoQuery page);
}
