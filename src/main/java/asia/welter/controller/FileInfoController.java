package asia.welter.controller;

import asia.welter.annotation.GlobalInterceptor;
import asia.welter.annotation.VerifyParam;
import asia.welter.entity.constants.Constants;
import asia.welter.entity.dto.SessionWebUserDto;
import asia.welter.entity.enums.FileCategoryEnums;
import asia.welter.entity.enums.FileDelFlagEnums;
import asia.welter.entity.query.FileInfoQuery;
import asia.welter.entity.vo.PaginationResultVO;
import asia.welter.entity.vo.ResponseVo;
import asia.welter.service.FileInfoService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController("FileInfoController")
@RequestMapping("/file")
public class FileInfoController {

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
}
