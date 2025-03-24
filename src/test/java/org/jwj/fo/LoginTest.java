package org.jwj.fo;

import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.jwj.fo.controller.UserController;
import org.jwj.fo.dto.LoginFormDTO;
import org.jwj.fo.dto.Result;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Random;

import static org.jwj.fo.utils.RedisConstants.LOGIN_CODE_KEY;
@Slf4j
@SpringBootTest
public class LoginTest {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserController userController;
    private static final String FILE_PATH = "accounts.txt";
    @Test
    public void test() throws Exception {
        // 创建写入器
        BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH));
        Random random = new Random();

        try {
            for (int i = 0; i < 200; i++) {
                // 生成随机手机号
                String phone = "1" + (random.nextInt(7) + 3) + RandomUtil.randomNumbers(9);

                // 发送验证码
                Result codeResult = userController.sendCode(phone, null);
                if (!codeResult.getSuccess()) {
                    continue;
                }

                // 从Redis获取验证码
                String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
                if (code == null) {
                    continue;
                }

                // 验证码登录
                LoginFormDTO loginFormDTO = new LoginFormDTO();
                loginFormDTO.setPhone(phone);
                loginFormDTO.setCode(code);

                Result loginResult = userController.login(loginFormDTO, null);
                if (!loginResult.getSuccess()) {
                    continue;
                }

                // 保存账号信息
                String token = loginResult.getData().toString();
                writer.write(String.format("%s,%s%n", phone, token));

                // 避免请求过快
                Thread.sleep(100);
            }
        } catch (Exception e) {
            log.error("批量注册失败", e);
        } finally {
            writer.close();
        }
    }
//    从文件中处理accounts.txt去除文件中的电话部分, 仅保留token
@Test
public void extractTokens() throws Exception {
    String outputPath = "tokens.txt";
    BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
    BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

    try {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length > 1) {
                writer.write(parts[1]);
                writer.newLine();
            }
        }
        log.info("Token extraction completed. Output file: {}", outputPath);
    } catch (Exception e) {
        log.error("Token extraction failed", e);
    } finally {
        reader.close();
        writer.close();
    }
}

}

