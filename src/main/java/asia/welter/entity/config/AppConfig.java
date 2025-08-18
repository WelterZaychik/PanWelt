package asia.welter.entity.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("appConfig")
@Data
public class AppConfig {

    @Value("${admin.emails:}")
    private String adminEmails;

    @Value("${spring.mail.username:}")
    private String sendUserName;

    @Value("${project.folder:}")
    private String projectFolder;


}
