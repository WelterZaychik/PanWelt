package asia.welter.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Target({ElementType.METHOD})//目标是什么
@Retention(RetentionPolicy.RUNTIME)//执行时机
@Documented
@Mapping
public @interface GlobalInterceptor {
    /**
     * 校验参数
     * @return
     */
    boolean checkParams() default false;

    /**
     * 校验登录
     */
    boolean checkLogin() default true;

    /**
     * 校验超级管理员
     */
    boolean checkAdmin() default false;

}
