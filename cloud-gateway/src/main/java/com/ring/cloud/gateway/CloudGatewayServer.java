package com.ring.cloud.gateway;

//import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
//import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

//@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
//@EnableFeignClients
//@ComponentScan(basePackages = {"com.ring.cloud", "com.ring.welkin"})
@EntityScan(basePackages = {"com.ring.welkin.**.entity", "com.ring.welkin.**.entity"})
@PropertySource(value = {"classpath:common.properties", "classpath:cloud-gateway.properties"}, ignoreResourceNotFound = true)
public class CloudGatewayServer {

    public static void main(String[] args) {
//        log.info("JAVA CLASS PATH = " + System.getProperty("java.class.path"));
        SpringApplication.run(CloudGatewayServer.class, args);
    }
}