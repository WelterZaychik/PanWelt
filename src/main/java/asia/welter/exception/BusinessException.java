package asia.welter.exception;

import asia.welter.entity.enums.ResponseCodeEnum;
import lombok.Getter;


/**
 * 业务异常类
 * <p>
 * 用于处理业务逻辑中的异常情况，通常不需要打印堆栈信息以提高性能
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 响应码枚举
     */
    private final ResponseCodeEnum codeEnum;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;

    /**
     * 构造方法 - 使用消息和原因
     *
     * @param message 错误消息
     * @param cause   原因异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.code = null;
        this.codeEnum = null;
    }

    /**
     * 构造方法 - 仅使用消息
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.message = message;
        this.code = null;
        this.codeEnum = null;
    }

    /**
     * 构造方法 - 仅使用原因
     *
     * @param cause 原因异常
     */
    public BusinessException(Throwable cause) {
        super(cause);
        this.message = cause.getMessage();
        this.code = null;
        this.codeEnum = null;
    }

    /**
     * 构造方法 - 使用响应码枚举
     *
     * @param codeEnum 响应码枚举
     */
    public BusinessException(ResponseCodeEnum codeEnum) {
        super(codeEnum.getMsg());
        this.codeEnum = codeEnum;
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMsg();
    }

    /**
     * 构造方法 - 使用响应码枚举和原因
     *
     * @param codeEnum 响应码枚举
     * @param cause    原因异常
     */
    public BusinessException(ResponseCodeEnum codeEnum, Throwable cause) {
        super(codeEnum.getMsg(), cause);
        this.codeEnum = codeEnum;
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMsg();
    }

    /**
     * 构造方法 - 使用自定义错误码和消息
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.codeEnum = null;
    }

    /**
     * 构造方法 - 使用自定义错误码、消息和原因
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原因异常
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.codeEnum = null;
    }

    /**
     * 重写fillInStackTrace - 业务异常通常不需要堆栈信息，提高效率
     *
     * @return 当前异常实例
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    /**
     * 判断是否包含响应码枚举
     *
     * @return 是否包含响应码枚举
     */
    public boolean hasCodeEnum() {
        return this.codeEnum != null;
    }
}