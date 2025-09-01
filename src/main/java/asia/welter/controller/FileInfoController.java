package asia.welter.controller;

import asia.welter.annotation.GlobalInterceptor;
import asia.welter.annotation.VerifyParam;
import asia.welter.entity.constants.Constants;
import asia.welter.entity.dto.SessionWebUserDto;
import asia.welter.entity.dto.UploadResultDto;
import asia.welter.entity.enums.FileCategoryEnums;
import asia.welter.entity.enums.FileDelFlagEnums;
import asia.welter.entity.enums.FileFolderTypeEnums;
import asia.welter.entity.po.FileInfo;
import asia.welter.entity.query.FileInfoQuery;
import asia.welter.entity.vo.FileInfoVO;
import asia.welter.entity.vo.PaginationResultVO;
import asia.welter.entity.vo.ResponseVo;
import asia.welter.service.FileInfoService;
import asia.welter.utils.CopyTools;
import asia.welter.utils.StringTools;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

@RestController("FileInfoController")
@RequestMapping("/file")
public class FileInfoController extends CommonFileController{

    @Autowired
    private FileInfoService fileInfoService;

    @PostMapping("loadDataList")
    @GlobalInterceptor(checkParams = true)
    public ResponseVo loadDataList(HttpSession session, @VerifyParam(required = true) FileInfoQuery query, @VerifyParam(required = true)String category) {
        FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
        if(null != categoryEnum) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        query.setUserId(sessionWebUserDto.getUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO resultVO = fileInfoService.findListByPage(query);
        return ResponseVo.success(resultVO);
    }

    @PostMapping("uploadFile")
    public ResponseVo uploadFile(HttpSession session,
                                 String fileId,
                                 MultipartFile file,
                                 @VerifyParam(required = true) String fileName,
                                 @VerifyParam(required = true) String filePid,
                                 @VerifyParam(required = true) String fileMd5,
                                 @VerifyParam(required = true) Integer chunkIndex,
                                 @VerifyParam(required = true) Integer chunks) {

        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        UploadResultDto resultDto = fileInfoService.uploadFile(webUserDto, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
        return ResponseVo.success(resultDto);
    }

    @GetMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder, @PathVariable("imageName") String imageName) {
        super.getImage(response, imageFolder, imageName);
    }

    @GetMapping("/ts/getVideoInfo/{fileId}")
    public void getVideoInfo(HttpServletResponse response, HttpSession session, @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        super.getFile(response, fileId, sessionWebUserDto.getUserId());
    }

    @GetMapping("/getFile/{fileId}")
    public void getFile(HttpServletResponse response, HttpSession session, @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        super.getFile(response, fileId, sessionWebUserDto.getUserId());
    }


    @PostMapping("newFoloder")
    public ResponseVo newFoloder(HttpSession session,@VerifyParam(required = true) String filePid,
                                 @VerifyParam(required = true) String fileName) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        FileInfo fileInfo = fileInfoService.newFolder(filePid,sessionWebUserDto.getUserId(),fileName);
        return ResponseVo.success(fileInfo);
    }

    @PostMapping("getFolderInfo")
    public ResponseVo getFolderInfo(HttpSession session,@VerifyParam(required = true) String path) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        return super.getFolderInfo(path,sessionWebUserDto.getUserId());
    }

    @PostMapping("rename")
    @GlobalInterceptor(checkParams = true)
    public ResponseVo rename(HttpSession session,
                             @VerifyParam(required = true) String fileId,
                             @VerifyParam(required = true) String fileName) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        FileInfo fileInfo = fileInfoService.rename(fileId, webUserDto.getUserId(), fileName);
        return ResponseVo.success(CopyTools.copy(fileInfo, FileInfoVO.class));
    }


    @PostMapping("loadAllFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVo loadAllFolder(HttpSession session,@VerifyParam(required = true) String filePid, String currentFileIds) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        fileInfoQuery.setUserId(webUserDto.getUserId());
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        if (!StringTools.isEmpty(currentFileIds)) {
            fileInfoQuery.setExcludeFileIdArray(currentFileIds.split(","));
        }
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoQuery.setOrderBy("create_time desc");
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(fileInfoQuery);
        return ResponseVo.success(CopyTools.copyList(fileInfoList, FileInfoVO.class));

    }

    @PostMapping("changeFileFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVo changeFileFolder(HttpSession session,
                                       @VerifyParam(required = true) String fileIds,
                                       @VerifyParam(required = true) String filePid) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        fileInfoService.changeFileFolder(fileIds, filePid, webUserDto.getUserId());
        return ResponseVo.success(null);
    }

}
