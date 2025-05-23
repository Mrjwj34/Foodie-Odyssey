package org.jwj.fo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("org.jwj.fo.mapper")
@SpringBootApplication
public class FoodieOdysseyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodieOdysseyApplication.class, args);
    }

}
