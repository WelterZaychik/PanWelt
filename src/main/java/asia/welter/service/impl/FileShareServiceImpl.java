package asia.welter.service.impl;


import asia.welter.entity.po.FileShare;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import asia.welter.service.FileShareService;
import asia.welter.mapper.FileShareMapper;
import org.springframework.stereotype.Service;

/**
* @author Welt
* @description 针对表【file_share(分享信息)】的数据库操作Service实现
* @createDate 2025-08-23 15:44:32
*/
@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare>
    implements FileShareService{

}




