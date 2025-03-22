package org.jwj.fo;

import org.junit.jupiter.api.Test;
import org.jwj.fo.utils.RedisIdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class TestRedisWorker {
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Test
    void testNextId() {
        ExecutorService es = Executors.newFixedThreadPool(300);
        CountDownLatch countDownLatch = new CountDownLatch(300);
        long start = System.currentTimeMillis();

        for (int i = 0; i < 300; i++) {
            es.submit(() -> {
                for (int j = 0; j < 10; j++) {
                    long id = redisIdWorker.nextId("test");
                    System.out.println("id: " + id);
                }
                countDownLatch.countDown();
            });
        }

        try {
            countDownLatch.await();
            es.shutdown();
            System.out.println("耗时：" + (System.currentTimeMillis() - start));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
