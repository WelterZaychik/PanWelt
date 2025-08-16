package asia.welter.controller;

import asia.welter.annotation.GlobalInterceptor;
import asia.welter.annotation.VerifyParam;
import asia.welter.entity.constants.Constants;
import asia.welter.entity.dto.CreateImageCode;
import asia.welter.entity.dto.SessionWebUserDto;
import asia.welter.entity.enums.VerifyRegexEnum;
import asia.welter.entity.vo.ResponseVo;
import asia.welter.exception.BusinessException;
import asia.welter.service.EmailCodeService;
import asia.welter.service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static asia.welter.entity.vo.ResponseVo.fail;
import static asia.welter.entity.vo.ResponseVo.success;

@RestController("AccountController")
//@RequestMapping("User")
public class AccountController {
    @Autowired
    private UsersService usersService;

    @Autowired
    private EmailCodeService emailCodeService;

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
    @GlobalInterceptor(checkParams = true)
    public ResponseVo sendEmailCode(HttpSession session, @VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL,max = 150) String email
            , @VerifyParam(required = true) String checkCode
            , @VerifyParam(required = true)Integer type){
        try{

            if (!checkCode.equalsIgnoreCase(session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL).toString())){
                throw new BusinessException("图片验证码不正确");
//                return fail(505,"图片验证码不正确");
            }

            emailCodeService.sendEmailCode(email,type);
            return success(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }

    }

    @PostMapping("register")
    @GlobalInterceptor(checkParams = true)
    public ResponseVo register(HttpSession session
            , @VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL,max = 150) String email
            , @VerifyParam(required = true) String nickName
            , @VerifyParam(required = true,min = 8 , max = 10) String password
            , @VerifyParam(required = true) String checkCode
            , @VerifyParam(required = true) String emailCode){
        try{


            if (!checkCode.equalsIgnoreCase(session.getAttribute(Constants.CHECK_CODE_KEY).toString())){
                throw new BusinessException("图片验证码不正确");

            }

            usersService.register(email,nickName,password,emailCode);
            return success(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }

    }

    @PostMapping("login")
    @GlobalInterceptor(checkParams = true)
    public ResponseVo login(HttpSession session
            , @VerifyParam(required = true) String email
            , @VerifyParam(required = true) String password
            , @VerifyParam(required = true) String checkCode){
        try{

            if (!checkCode.equalsIgnoreCase(session.getAttribute(Constants.CHECK_CODE_KEY).toString())){
                throw new BusinessException("图片验证码不正确");

            }

            SessionWebUserDto login = usersService.login(email, password);
            session.setAttribute(Constants.SESSION_KEY,login);
            return success(login);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }

    }
}