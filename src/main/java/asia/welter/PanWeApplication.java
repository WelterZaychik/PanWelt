package asia.welter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@EnableTransactionManagement
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"asia.welter"})
@MapperScan(basePackages = {"asia.welter.mapper"})
public class PanWeApplication {
    public static void main(String[] args) {
        SpringApplication.run(PanWeApplication.class, args);
    }
}
