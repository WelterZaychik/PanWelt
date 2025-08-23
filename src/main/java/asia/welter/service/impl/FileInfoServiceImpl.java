package asia.welter.service.impl;



import asia.welter.entity.po.FileInfo;
import asia.welter.entity.query.FileInfoQuery;
import asia.welter.entity.vo.PaginationResultVO;
import asia.welter.mapper.FileInfoMapper;
import asia.welter.service.FileInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author Welt
* @description 针对表【file_info(文件信息)】的数据库操作Service实现
* @createDate 2025-08-23 15:44:13
*/
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo>
    implements FileInfoService {

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Override
    public PaginationResultVO findListByPage(FileInfoQuery query) {
        int pageSize = query.getPageSize()==null ? 15 : query.getPageNo();
        int pageNo = query.getPageNo()==null ? 1 : query.getPageNo();
        Page<FileInfo> page = new Page(pageNo, pageSize);
//        Page<FileInfo> page = query.toPage();
        query.handleOrderBy(page);
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FileInfo::getLastUpdateTime);
        Page<FileInfo> resultPage = fileInfoMapper.selectPage(page,wrapper);

        PaginationResultVO resultVO =new PaginationResultVO(
                (int) resultPage.getTotal(),   // totalCount
                (int) resultPage.getPages(),   // pageSize，
                (int) resultPage.getCurrent(), // pageNo
                (int) resultPage.getTotal(),   //  pageTotal
                resultPage.getRecords()        // list
        );


        return resultVO;
    }
}




