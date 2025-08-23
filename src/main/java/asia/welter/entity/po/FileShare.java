package asia.welter.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 分享信息
 * @TableName file_share
 */
@TableName(value ="file_share")
@Data
public class FileShare {
    /**
     * 分享ID
     */
    @TableId
    private String shareId;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 有效期类型 0:1天 1:7天 2:30天 3:永久有效
     */
    private Integer validType;

    /**
     * 失效时间
     */
    private LocalDateTime expireTime;

    /**
     * 分享时间
     */
    private LocalDateTime shareTime;

    /**
     * 提取码
     */
    private String code;

    /**
     * 浏览次数
     */
    private Integer showCount;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        FileShare other = (FileShare) that;
        return (this.getShareId() == null ? other.getShareId() == null : this.getShareId().equals(other.getShareId()))
            && (this.getFileId() == null ? other.getFileId() == null : this.getFileId().equals(other.getFileId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getValidType() == null ? other.getValidType() == null : this.getValidType().equals(other.getValidType()))
            && (this.getExpireTime() == null ? other.getExpireTime() == null : this.getExpireTime().equals(other.getExpireTime()))
            && (this.getShareTime() == null ? other.getShareTime() == null : this.getShareTime().equals(other.getShareTime()))
            && (this.getCode() == null ? other.getCode() == null : this.getCode().equals(other.getCode()))
            && (this.getShowCount() == null ? other.getShowCount() == null : this.getShowCount().equals(other.getShowCount()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getShareId() == null) ? 0 : getShareId().hashCode());
        result = prime * result + ((getFileId() == null) ? 0 : getFileId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getValidType() == null) ? 0 : getValidType().hashCode());
        result = prime * result + ((getExpireTime() == null) ? 0 : getExpireTime().hashCode());
        result = prime * result + ((getShareTime() == null) ? 0 : getShareTime().hashCode());
        result = prime * result + ((getCode() == null) ? 0 : getCode().hashCode());
        result = prime * result + ((getShowCount() == null) ? 0 : getShowCount().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", shareId=").append(shareId);
        sb.append(", fileId=").append(fileId);
        sb.append(", userId=").append(userId);
        sb.append(", validType=").append(validType);
        sb.append(", expireTime=").append(expireTime);
        sb.append(", shareTime=").append(shareTime);
        sb.append(", code=").append(code);
        sb.append(", showCount=").append(showCount);
        sb.append("]");
        return sb.toString();
    }
}