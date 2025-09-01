package asia.welter.service.impl;



import asia.welter.component.RedisComponent;
import asia.welter.entity.config.AppConfig;
import asia.welter.entity.constants.Constants;
import asia.welter.entity.dto.SessionWebUserDto;
import asia.welter.entity.dto.UploadResultDto;
import asia.welter.entity.dto.UserSpaceDto;
import asia.welter.entity.enums.*;
import asia.welter.entity.po.FileInfo;
import asia.welter.entity.query.FileInfoQuery;
import asia.welter.entity.query.SimplePage;
import asia.welter.entity.vo.PaginationResultVO;
import asia.welter.exception.BusinessException;
import asia.welter.mapper.FileInfoMapper;
import asia.welter.mapper.UsersMapper;
import asia.welter.service.FileInfoService;
import asia.welter.service.UsersService;
import asia.welter.utils.ProcessUtils;
import asia.welter.utils.ScaleFilter;
import asia.welter.utils.StringTools;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author Welt
* @description 针对表【file_info(文件信息)】的数据库操作Service实现
* @createDate 2025-08-23 15:44:13
*/
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo>
    implements FileInfoService {

    private static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    @Lazy  // 延迟注入，避免循环依赖
    private FileInfoServiceImpl fileInfoService;

    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private UsersMapper usersMapper;
    @Autowired
    private AppConfig appConfig;

    @Override
    public PaginationResultVO findListByPage(FileInfoQuery query) {
        int count = this.findCountByParam(query);
        int pageSize = query.getPageSize() == null ? 15 : query.getPageSize();

        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<FileInfo> list = this.findListByParam(query);
        PaginationResultVO<FileInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks) {
        File temFileFolder = null;
        Boolean isSuccess = true;

        try {
            UploadResultDto resultDto = new UploadResultDto();
            if (StringTools.isEmpty(fileId)){
                fileId = StringTools.getRandomString(Constants.LENGTH_10);
            }
            resultDto.setFileId(fileId);
            LocalDateTime currentTime = LocalDateTime.now();
            UserSpaceDto userSpaceDto = redisComponent.getUserSpaceUse(webUserDto.getUserId());

            //秒传逻辑
            if(chunkIndex == 0){
                FileInfoQuery fileInfoQuery = new FileInfoQuery();
                fileInfoQuery.setFileMd5(fileMd5);
                fileInfoQuery.setStatus(FileStatusEnums.USING.getStatus());
                FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileMd5,fileMd5).eq(FileInfo::getStatus,FileStatusEnums.USING.getStatus()));

                //判断数据库中是否已存在
                if (fileInfo != null){
                    if (fileInfo.getFileSize() + userSpaceDto.getUseSpace() > userSpaceDto.getTotalSpace()){
                        throw new BusinessException(ResponseCodeEnum.CODE_904);
                    }
                    fileInfo.setFileId(fileId);
                    fileInfo.setFilePid(filePid);
                    fileInfo.setUserId(webUserDto.getUserId());
                    fileInfo.setFileMd5(fileMd5);
                    fileInfo.setCreateTime(currentTime);
                    fileInfo.setLastUpdateTime(currentTime);
                    fileInfo.setStatus(FileStatusEnums.USING.getStatus());
                    fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());

                    fileName = autoRename(filePid,webUserDto.getUserId(),fileName);

                    fileInfo.setFileName(fileName);
                    fileInfoMapper.insert(fileInfo);
                    resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
                    updateUserSpace(webUserDto, fileInfo.getFileSize());

                    return resultDto;
                }
            }

            //暂存的临时目录地址
            String tempFolderPath = appConfig.getProjectFolder()+Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = webUserDto.getUserId()+fileId;

            //创建临时目录
            temFileFolder = new File(tempFolderPath+currentUserFolderName);
            if (!temFileFolder.exists()){
                temFileFolder.mkdirs();
            }

            //判断磁盘空间大小
            Long currentTempSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
            if (file.getSize()+currentTempSize > userSpaceDto.getTotalSpace()){
                throw new BusinessException(ResponseCodeEnum.CODE_904);
            }

            File newFile = new File(temFileFolder.getPath()+"/"+chunkIndex);
            file.transferTo(newFile);

            //保存临时存储大小
            redisComponent.saveFileTempSize(webUserDto.getUserId(),fileId,file.getSize());

            //判断是否是最后一个分片
            if(chunkIndex < chunks - 1){
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                return resultDto;
            }

            //已经传输完毕
            String month = currentTime.format(DateTimeFormatter.ofPattern("yyyyMM"));
            String fileSuffix = StringTools.getFileSuffix(fileName);

            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);

            //重命名以及修改存储dto
            fileName = autoRename(filePid,webUserDto.getUserId(),fileName);
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(webUserDto.getUserId());
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileName(fileName);
            fileInfo.setFilePath(month + "/" + realFileName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(currentTime);
            fileInfo.setLastUpdateTime(currentTime);
            fileInfo.setFileCategory(fileTypeEnum.getCategory().getCategory());
            fileInfo.setFileType(fileTypeEnum.getType());
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            fileInfoMapper.insert(fileInfo);

            Long totalSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
            updateUserSpace(webUserDto, totalSize);

            resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());

            //事务提交
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoService.transferFile(fileInfo.getFileId(), webUserDto);
                }
            });
            return resultDto;

        } catch (BusinessException e) {
            isSuccess = false;
            logger.error("文件上传失败", e);
            throw e;
        } catch (Exception e) {
            isSuccess = false;
            logger.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        } finally {
            //如果上传失败，清除临时目录
            if (temFileFolder != null && !isSuccess) {
                try {
                    FileUtils.deleteDirectory(temFileFolder);
                } catch (IOException e) {
                    logger.error("删除临时目录失败");
                }
            }
        }
    }

    @Override
    public Long getUserUseSpace(String userId) {
        return fileInfoMapper.selectUserUsedSpace(userId);
    }

    @Override
    public FileInfo getFileInfoByFileIdAndUserId(String realFileId, String userId) {

        return fileInfoMapper.selectByFileIdAndUserId(realFileId,userId);
    }


    @Override
    public List<FileInfo> findListByParam(FileInfoQuery fileInfoQuery) {
        return fileInfoMapper.selectFileList(fileInfoQuery);
    }

    @Override
    public Integer findCountByParam(FileInfoQuery fileInfoQuery) {
        long count = fileInfoMapper.selectCount(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag())
                .eq(fileInfoQuery.getUserId() != null, FileInfo::getUserId, fileInfoQuery.getUserId())
                .eq(fileInfoQuery.getFilePid() != null, FileInfo::getFilePid, fileInfoQuery.getFilePid())
                .eq(StringUtils.isNotBlank(fileInfoQuery.getFileName()), FileInfo::getFileName, fileInfoQuery.getFileName())
                .eq(fileInfoQuery.getFileType() != null, FileInfo::getFileType, fileInfoQuery.getFileType())
                .eq(fileInfoQuery.getStatus() != null, FileInfo::getStatus, fileInfoQuery.getStatus())
                .eq(fileInfoQuery.getFileCategory() != null, FileInfo::getFileCategory, fileInfoQuery.getFileCategory())
                .ge(fileInfoQuery.getCreateTimeStart() != null, FileInfo::getCreateTime, fileInfoQuery.getCreateTimeStart())
                .le(fileInfoQuery.getCreateTimeEnd() != null, FileInfo::getCreateTime, fileInfoQuery.getCreateTimeEnd()));

        return Math.toIntExact(count);
    }

    @Override
    public FileInfo newFolder(String filePid, String userId, String folderName) {
        checkFileName(filePid, userId, folderName, FileFolderTypeEnums.FOLDER.getType());
        LocalDateTime now = LocalDateTime.now();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomString(Constants.LENGTH_10));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileName(folderName);
        fileInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfo.setCreateTime(now);
        fileInfo.setLastUpdateTime(now);
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoMapper.insert(fileInfo);

        long count = this.fileInfoMapper.selectCount(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFileName, folderName)
                .eq(FileInfo::getFolderType, FileFolderTypeEnums.FOLDER.getType())
                .eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag()));
        if (count > 1) {
            throw new BusinessException("文件夹" + folderName + "已经存在");
        }
        fileInfo.setFileName(folderName);
        fileInfo.setLastUpdateTime(now);
        return fileInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo rename(String fileId, String userId, String fileName) {
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userId);

        if (fileInfo == null) {
            throw new BusinessException("文件被删除或不存在");
        }
        if (fileInfo.getFileName().equals(fileName)) {
            return fileInfo; // 文件名未变化，直接返回
        }

        String filePid = fileInfo.getFilePid();
        checkFileName(filePid, userId, fileName, fileInfo.getFolderType());


        // 3. 处理文件后缀（如果是文件类型）
        if (FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFolderType())) {
            String suffix = StringTools.getFileSuffix(fileInfo.getFileName());
            if (!fileName.endsWith(suffix)) { // 防止重复追加
                fileName = fileName + suffix;
            }
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<FileInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FileInfo::getFileId, fileId)
                .eq(FileInfo::getUserId, userId)
                .set(FileInfo::getFileName, fileName)
                .set(FileInfo::getLastUpdateTime, now);
        int update = fileInfoMapper.update(null, updateWrapper);// 传入 null 表示不依赖实体对象
        if (update != 1) {
            throw new BusinessException("服务器繁忙，修改文件名失败");
        }

        fileInfo.setFileName(fileName);
        fileInfo.setLastUpdateTime(now);
        return fileInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeFileFolder(String fileIds, String filePid, String userId) {
        if (fileIds.equals(filePid)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (Constants.ZERO_STR.equals(filePid)){
            FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileIds, userId);
            if (fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        String[] fileIdArray = fileIds.split(",");

        FileInfoQuery query = new FileInfoQuery();
        query.setFilePid(filePid);
        query.setUserId(userId);
        List<FileInfo> dbFileList = fileInfoService.findListByParam(query);

        Map<String, FileInfo> dbFileNameMap = dbFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));
        //查询选中的文件
        query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        List<FileInfo> selectFileList = fileInfoService.findListByParam(query);

        LambdaUpdateWrapper<FileInfo> updateWrapper = null;
        //将所选文件重命名
        for (FileInfo item : selectFileList) {
            FileInfo rootFileInfo = dbFileNameMap.get(item.getFileName());
            updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(FileInfo::getFileId, item.getFileId())
                    .eq(FileInfo::getUserId, userId);
            //文件名已经存在，重命名被还原的文件名

            if (rootFileInfo != null) {
                String fileName = StringTools.rename(item.getFileName());
                updateWrapper .set(FileInfo::getFileName, fileName);
            }
            updateWrapper.set(FileInfo::getFilePid,filePid);

            fileInfoMapper.update(null, updateWrapper);
        }

    }


    /**
     * 处理排序条件，防止SQL注入
     */
    private void handleOrderBy(LambdaQueryWrapper<FileInfo> wrapper, String orderBy) {
        // 定义允许排序的字段白名单
        Map<String, SFunction<FileInfo, ?>> allowedOrderFields = new HashMap<>();
        allowedOrderFields.put("create_time", FileInfo::getCreateTime);
        allowedOrderFields.put("file_size", FileInfo::getFileSize);
        allowedOrderFields.put("file_name", FileInfo::getFileName);
        allowedOrderFields.put("last_update_time", FileInfo::getLastUpdateTime);

        // 解析排序字段和方向
        String[] parts = orderBy.split(" ");
        String field = parts[0];
        String direction = parts.length > 1 ? parts[1] : "ASC";

        // 检查字段是否在白名单中
        SFunction<FileInfo, ?> column = allowedOrderFields.get(field.toLowerCase());
        if (column != null) {
            if ("DESC".equalsIgnoreCase(direction)) {
                wrapper.orderByDesc(column);
            } else {
                wrapper.orderByAsc(column);
            }
        } else {
            // 字段不在白名单中，使用默认排序
            wrapper.orderByDesc(FileInfo::getCreateTime);
        }
    }



    private void updateUserSpace(SessionWebUserDto webUserDto, Long tempSize) {
        Integer count = usersMapper.updateUserSpace(webUserDto.getUserId(), tempSize, null);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(webUserDto.getUserId());
        spaceDto.setUseSpace(spaceDto.getUseSpace() + tempSize);
        redisComponent.saveUserSpaceUse(webUserDto.getUserId(), spaceDto);
    }

    private String autoRename(String filePid, String userId, String fileName) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoQuery.setFileName(fileName);
        Integer count = findCountByParam(fileInfoQuery);
        if (count > 0) {
            return StringTools.rename(fileName);
        }

        return fileName;
    }

    @Async("applicationTaskExecutor")
    public void transferFile(String fileId, SessionWebUserDto webUserDto) {
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnums fileTypeEnum = null;

        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, webUserDto.getUserId());

        try {
            if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            //临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = webUserDto.getUserId() + fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);
            if (!fileFolder.exists()) {
                fileFolder.mkdirs();
            }
            //文件后缀
            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
//            String month = DateUtil.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());
            String month = fileInfo.getCreateTime().format(DateTimeFormatter.ofPattern(DateTimePatternEnum.YYYYMM.getPattern()));
            //目标目录
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName + "/" + month);
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            //真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            //真实文件路径
            targetFilePath = targetFolder.getPath() + "/" + realFileName;
            //合并文件
            union(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);
            //视频文件切割
            fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            if (FileTypeEnums.VIDEO == fileTypeEnum) {
                cutFile4Video(fileId, targetFilePath);
                //视频生成缩略图
                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                ScaleFilter.createCover4Video(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath));
            } else if (FileTypeEnums.IMAGE == fileTypeEnum) {
                //生成缩略图
                cover = month + "/" + realFileName.replace(".", "_.");
                String coverPath = targetFolderName + "/" + cover;
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
                if (!created) {
                    FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
                }
            }
        } catch (Exception e) {
            logger.error("文件转码失败，文件Id:{},userId:{}", fileId, webUserDto.getUserId(), e);
            transferSuccess = false;
        } finally {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setFileSize(new File(targetFilePath).length());
            updateInfo.setFileCover(cover);
            updateInfo.setStatus(transferSuccess ? FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER_FAIL.getStatus());
            fileInfoMapper.updateFileStatusWithOldStatus(fileId, webUserDto.getUserId(), updateInfo, FileStatusEnums.TRANSFER.getStatus());
        }
    }

    public static void union(String dirPath, String toFilePath, String fileName, boolean delSource) throws BusinessException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        File fileList[] = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile, "rw");
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                //创建读块文件的对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    logger.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            logger.error("合并文件:{}失败", fileName, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        } finally {
            try {
                if (null != writeFile) {
                    writeFile.close();
                }
            } catch (IOException e) {
                logger.error("关闭流失败", e);
            }
            if (delSource) {
                if (dir.exists()) {
                    try {
                        FileUtils.deleteDirectory(dir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void cutFile4Video(String fileId, String videoFilePath) {
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";

        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        //生成.ts
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);
        //生成索引文件.m3u8 和切片.ts
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);
        //删除index.ts
        new File(tsPath).delete();
    }

    private void checkFileName(String filePid, String userId, String fileName, Integer folderType) {
        long count = this.fileInfoMapper.selectCount(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getFolderType, folderType)
                .eq(FileInfo::getFileName, fileName)
                .eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag()));
        if (count > 0) {
            throw new BusinessException("此目录下已存在同名文件，请修改名称");
        }
    }
}




