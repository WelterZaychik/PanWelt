package asia.welter.entity.query;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class BaseParam {
    private SimplePage simplePage;
    private Integer pageNo = 1;      // 默认第一页
    private Integer pageSize = 20;   // 默认每页20条
    private String orderBy;

    // 转换为 MyBatis-Plus 的 Page 对象
    public <T> Page<T> toPage() {
        return new Page<>(pageNo, pageSize);
    }

    // 处理排序字段
    public void handleOrderBy(Page<?> page) {
        if (StringUtils.isNotBlank(orderBy)) {
            // 将 "create_time desc" 转换为 MP 的排序方式
            String[] orderArr = orderBy.split(" ");
            if (orderArr.length == 2) {
                if ("desc".equalsIgnoreCase(orderArr[1])) {
                    page.addOrder(OrderItem.desc(orderArr[0]));
                } else {
                    page.addOrder(OrderItem.asc(orderArr[0]));
                }
            }
        }
    }

}