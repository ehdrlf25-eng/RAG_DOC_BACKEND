package com.ragdoc.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** AWS S3 클라이언트 ({@code cloud} 프로필). EC2 IAM Role 기본 자격 증명 체인을 사용한다. */
@Configuration
@Profile("cloud")
public class AwsS3Config {

    @Bean
    public S3Client s3Client(RagProperties ragProperties) {
        String region = ragProperties.storage().region();
        if (region == null || region.isBlank()) {
            region = "ap-northeast-2";
        }
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
