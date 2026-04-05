package ru.nand.vktesttask.config;

import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.factory.TarantoolBoxClientBuilder;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.pool.InstanceConnectionGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class TarantoolConfig {

    @Value("${tarantool.host:localhost}")
    private String host;

    @Value("${tarantool.port:3301}")
    private int port;

    @Value("${tarantool.username:admin}")
    private String username;

    @Value("${tarantool.password:admin}")
    private String password;

    @Bean(destroyMethod = "close")
    public TarantoolBoxClient tarantoolBoxClient() {
        try {
            InstanceConnectionGroup connectionGroup = InstanceConnectionGroup.builder()
                    .withHost(host)
                    .withPort(port)
                    .withUser(username)
                    .withPassword(password)
                    .build();

            TarantoolBoxClientBuilder builder = TarantoolFactory.box()
                    .withGroups(Collections.singletonList(connectionGroup));

            return builder.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create TBC", e);
        }
    }
}