package org.hanghae.markethub;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hanghae.markethub.domain.item.dto.RedisItemResponseDto;
import org.hanghae.markethub.domain.item.entity.Item;
import org.hanghae.markethub.global.config.AppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;


@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
@PropertySource("classpath:application.properties")
public class MarkethubApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarkethubApplication.class, args);
    }
}


