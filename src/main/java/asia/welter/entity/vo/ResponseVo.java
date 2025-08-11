package asia.welter.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;
import java.io.Serializable;

/**
 * 通用响应对象
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ResponseVo<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 状态标识
     */
    private String status;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String info;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     * @param data 响应数据
     * @return 响应对象
     */
    public static <T> ResponseVo<T> success(T data) {
        return new ResponseVo<T>()
                .setStatus("success")
                .setCode(200)
                .setInfo("操作成功")
                .setData(data);
    }

    /**
     * 成功响应
     * @param info 提示信息
     * @param data 响应数据
     * @return 响应对象
     */
    public static <T> ResponseVo<T> success(String info, T data) {
        return new ResponseVo<T>()
                .setStatus("success")
                .setCode(200)
                .setInfo(info)
                .setData(data);
    }

    /**
     * 失败响应
     * @param code 错误码
     * @param info 错误信息
     * @return 响应对象
     */
    public static <T> ResponseVo<T> fail(Integer code, String info) {
        return new ResponseVo<T>()
                .setStatus("fail")
                .setCode(code)
                .setInfo(info)
                .setData(null);
    }

    /**
     * 失败响应
     * @param info 错误信息
     * @return 响应对象
     */
    public static <T> ResponseVo<T> fail(String info) {
        return fail(500, info);
    }

    /**
     * 判断是否成功
     * @return 是否成功
     */
    public boolean isSuccess() {
        return "success".equals(this.status);
    }
}