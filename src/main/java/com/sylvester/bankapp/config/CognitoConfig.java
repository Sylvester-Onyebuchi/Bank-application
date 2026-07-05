package com.sylvester.bankapp.config;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoConfig {

    @Value("${aws.region}")
    private String region;
    @Value("${aws.secretKey}")
    private String secretKey;
    @Value("${aws.accessKey}")
    private String accessKey;


    @Bean
    public CognitoIdentityProviderClient cognitoIdentityProviderClient() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                accessKey,
                secretKey
        );
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(awsCredentials)
                )
                .build();
    }
}
