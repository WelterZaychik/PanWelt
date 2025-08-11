package asia.welter.utils;

import org.apache.commons.lang3.RandomStringUtils;

/*
* 生成随机数
* */
public class StringTools {
    public static final String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count,true,true);
    }
}
