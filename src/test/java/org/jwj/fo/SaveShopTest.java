package org.jwj.fo;  // 确保包路径正确

import org.junit.jupiter.api.Test;  // 使用JUnit 5
import org.springframework.boot.test.context.SpringBootTest;
import org.jwj.fo.service.impl.ShopServiceImpl;

import javax.annotation.Resource;

@SpringBootTest
public class SaveShopTest {
    @Resource
    private ShopServiceImpl shopServiceImpl;

    @Test
    void test() {  // JUnit 5不需要public修饰符
        shopServiceImpl.saveShop2Redis(1L, 10L);
    }
}