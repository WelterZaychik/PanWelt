package asia.welter.controller;

import asia.welter.entity.constants.Constants;
import asia.welter.entity.dto.CreateImageCode;
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
    public ResponseVo sendEmailCode(HttpSession session, String email, String checkCode, Integer type){
        try{

            if (!checkCode.equalsIgnoreCase(session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL).toString())){
                throw new BusinessException("图片验证码不正确");
            }

            emailCodeService.sendEmailCode(email,type);
            return success(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }


    }
}