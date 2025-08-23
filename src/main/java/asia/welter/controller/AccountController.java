package asia.welter.controller;

import asia.welter.annotation.GlobalInterceptor;
import asia.welter.annotation.VerifyParam;
import asia.welter.component.RedisComponent;
import asia.welter.entity.config.AppConfig;
import asia.welter.entity.constants.Constants;
import asia.welter.entity.dto.CreateImageCode;
import asia.welter.entity.dto.SessionWebUserDto;
import asia.welter.entity.dto.UserSpaceDto;
import asia.welter.entity.enums.VerifyRegexEnum;
import asia.welter.entity.po.Users;
import asia.welter.entity.vo.ResponseVo;
import asia.welter.exception.BusinessException;
import asia.welter.service.EmailCodeService;
import asia.welter.service.UsersService;
import asia.welter.utils.ImageResponseUtil;
import asia.welter.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;

import static asia.welter.entity.vo.ResponseVo.fail;
import static asia.welter.entity.vo.ResponseVo.success;

@RestController("AccountController")
//@RequestMapping("User")
public class AccountController {

    private final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private UsersService usersService;

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private RedisComponent redisComponent;

    @GetMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws
            IOException {
        CreateImageCode vCode =new CreateImageCode(130,38,5,10);
        response.setHeader("Pragma","no-cache");
        response.setHeader("Cache-Control","no-cache");
        response.setDateHeader("Expires",0);
        response.setContentType("image/jpeg");
        String code =vCode.getCode();
        if (type == null || type == 0){
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        }else{
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    @PostMapping("sendEmailCode")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVo sendEmailCode(HttpSession session, @VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL,max = 150) String email
            , @VerifyParam(required = true) String checkCode
            , @VerifyParam(required = true)Integer type){
        try{

            // 1. 安全获取验证码并校验
            Object checkCodeObj = session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL);
            if (checkCodeObj == null) {
                throw new BusinessException("验证码未生成或已过期");
            }

            // 2. 转换为字符串并比较（忽略大小写）
            String sessionCheckCode = checkCodeObj.toString();
            if (!sessionCheckCode.equalsIgnoreCase(checkCode)) {
                throw new BusinessException("图片验证码不正确");
            }

            emailCodeService.sendEmailCode(email,type);
            return success(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }

    }

    @PostMapping("register")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVo register(HttpSession session
            , @VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL,max = 150) String email
            , @VerifyParam(required = true) String nickName
            , @VerifyParam(required = true,min = 8 , max = 10) String password
            , @VerifyParam(required = true) String checkCode
            , @VerifyParam(required = true) String emailCode){
        try{


            // 1. 安全获取验证码并校验
            Object checkCodeObj = session.getAttribute(Constants.CHECK_CODE_KEY);
            if (checkCodeObj == null) {
                throw new BusinessException("验证码未生成或已过期");
            }

            // 2. 转换为字符串并比较（忽略大小写）
            String sessionCheckCode = checkCodeObj.toString();
            if (!sessionCheckCode.equalsIgnoreCase(checkCode)) {
                throw new BusinessException("图片验证码不正确");
            }

            usersService.register(email,nickName,password,emailCode);
            return success(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }

    }

    @PostMapping("login")
    @GlobalInterceptor(checkParams = true,checkLogin = false )
    public ResponseVo login(HttpSession session
            , @VerifyParam(required = true) String email
            , @VerifyParam(required = true) String password
            , @VerifyParam(required = true) String checkCode){
        try{

            // 1. 安全获取验证码并校验
            Object checkCodeObj = session.getAttribute(Constants.CHECK_CODE_KEY);
            if (checkCodeObj == null) {
                throw new BusinessException("验证码未生成或已过期");
            }

            // 2. 转换为字符串并比较（忽略大小写）
            String sessionCheckCode = checkCodeObj.toString();
            if (!sessionCheckCode.equalsIgnoreCase(checkCode)) {
                throw new BusinessException("图片验证码不正确");
            }

            SessionWebUserDto login = usersService.login(email, password);
            session.setAttribute(Constants.SESSION_KEY,login);
            return success(login);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }

    }

    @PostMapping("resetPwd")
    @GlobalInterceptor(checkParams = true,checkLogin = false )
    public ResponseVo resetPwd(HttpSession session
            , @VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL,max = 150) String email
            , @VerifyParam(required = true,min = 8 , max = 10) String password
            , @VerifyParam(required = true) String checkCode
            , @VerifyParam(required = true) String emailCode){
        try{


            // 1. 安全获取验证码并校验
            Object checkCodeObj = session.getAttribute(Constants.CHECK_CODE_KEY);
            if (checkCodeObj == null) {
                throw new BusinessException("验证码未生成或已过期");
            }

            // 2. 转换为字符串并比较（忽略大小写）
            String sessionCheckCode = checkCodeObj.toString();
            if (!sessionCheckCode.equalsIgnoreCase(checkCode)) {
                throw new BusinessException("图片验证码不正确");
            }

            usersService.resetPwd(email,password,emailCode);
            return success(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }

    }

    @GetMapping("/getAvatar/{userId}")
    @GlobalInterceptor(checkParams = true)
    public void resetPwd(HttpServletResponse response
            , @VerifyParam(required = true) @PathVariable("userId") String userId){


            String avatarFolderName =appConfig.getProjectFolder() + Constants.FILE_FOLDER_AVATAR_NAME;
            File folder = new File(avatarFolderName);
            if (!folder.exists()){
                folder.mkdirs();
            }
            String avatarPath = avatarFolderName+ userId + Constants.AVATAR_SUFFIX;
            File avatarFile = new File(avatarPath);

            if (!avatarFile.exists()){
                if (new File(avatarFolderName+Constants.AVATAR_DEFAULT).exists()){
//                    avatarPath = Constants.AVATAR_DEFAULT;
                    avatarPath = avatarFolderName+Constants.AVATAR_DEFAULT;
                }else{
                    throw new BusinessException("没有默认头像,请联系管理员");
                }
            }
            ImageResponseUtil.writeImageToResponse(response,avatarPath);

    }

    @GetMapping("getUserInfo")
    public ResponseVo getUserInfo(HttpSession session){
        SessionWebUserDto userInfo = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        return success(userInfo);
    }

    @PostMapping("getUseSpace")
    public ResponseVo getUseSpace(HttpSession session){
        SessionWebUserDto userInfo = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        UserSpaceDto userSpaceUse = redisComponent.getUserSpaceUse(userInfo.getUserId());
        return success(userSpaceUse);
    }

    @PostMapping("logout")
    public ResponseVo logout(HttpSession session){
        session.removeAttribute(Constants.SESSION_KEY);
        return success(null);
    }

    @PostMapping("/updateUserAvatar")
    @GlobalInterceptor
    public ResponseVo updateUserAvatar(HttpSession session, MultipartFile avatar) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()) {
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + webUserDto.getUserId() + Constants.AVATAR_SUFFIX);
        try {
            avatar.transferTo(targetFile);
        } catch (Exception e) {
            logger.error("上传头像失败", e);
        }


        Users userInfo = new Users();
        userInfo.setUserId(webUserDto.getUserId());
        userInfo.setAvatar("");
        usersService.updateById(userInfo);
        webUserDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY, webUserDto);
        return success(null);
    }

    @PostMapping("/updatePassword")
    @GlobalInterceptor(checkParams = true)
    public ResponseVo updatePassword(HttpSession session,
                                     @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        Users userInfo = new Users();
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfo.setUserId(webUserDto.getUserId());
        usersService.updateById(userInfo);
        return success(null);
    }



}