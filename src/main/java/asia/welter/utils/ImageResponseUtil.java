package asia.welter.utils;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ImageResponseUtil {

    private static final Map<String, String> IMAGE_TYPES = new HashMap<>();

    static {
        // 初始化常见图片格式的Content-Type映射
        IMAGE_TYPES.put("jpg", "image/jpeg");
        IMAGE_TYPES.put("jpeg", "image/jpeg");
        IMAGE_TYPES.put("png", "image/png");
        IMAGE_TYPES.put("gif", "image/gif");
        IMAGE_TYPES.put("webp", "image/webp");
        IMAGE_TYPES.put("bmp", "image/bmp");
        IMAGE_TYPES.put("svg", "image/svg+xml");
        IMAGE_TYPES.put("ico", "image/x-icon");
    }

    /**
     * 自动识别图片格式并输出到HTTP响应流
     * @param response HttpServletResponse对象
     * @param imagePath 图片文件路径
     * @return 是否输出成功
     */
    public static boolean writeImageToResponse(HttpServletResponse response, String imagePath) {
        // 参数校验
        if (response == null || imagePath == null) {
            return false;
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists() || !imageFile.isFile()) {
            return false;
        }

        // 自动识别图片格式
        String contentType = determineImageContentType(imagePath);
        if (contentType == null) {
            return false; // 不支持的图片格式
        }

        response.setContentType(contentType);

        // 使用try-with-resources自动关闭流
        try (InputStream in = new FileInputStream(imageFile);
             OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据文件扩展名自动识别图片Content-Type
     */
    private static String determineImageContentType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
        return IMAGE_TYPES.get(extension);
    }
}