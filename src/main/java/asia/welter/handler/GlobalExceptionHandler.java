package asia.welter.handler;

import asia.welter.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        // 根据业务码自动匹配HTTP状态码
        HttpStatus httpStatus = mapCodeToHttpStatus(ex.getCode());
        ErrorResponse response = new ErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, httpStatus);
    }

    /**
     * 业务码转HTTP状态码
     */
    private HttpStatus mapCodeToHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        switch (code) {
            case 200:
                return HttpStatus.OK;
            case 404:
                return HttpStatus.NOT_FOUND;
            case 600:
                return HttpStatus.BAD_REQUEST;
            case 901:
                return HttpStatus.UNAUTHORIZED;
            case 902:
            case 903:
                return HttpStatus.FORBIDDEN;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * 统一错误响应格式
     */
    @Data
    private static class ErrorResponse {
        private final Integer code;
        private final String message;
        private final LocalDateTime timestamp;
    }
}