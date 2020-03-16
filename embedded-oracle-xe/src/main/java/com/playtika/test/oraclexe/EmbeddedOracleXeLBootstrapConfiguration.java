/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.oraclexe;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.OracleContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.oraclexe.OracleXeProperties.BEAN_NAME_EMBEDDED_ORACLEXE;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.oracle-xe.enabled", matchIfMissing = true)
@EnableConfigurationProperties(OracleXeProperties.class)
public class EmbeddedOracleXeLBootstrapConfiguration {

    // TODO should be public in testcontainers-oracle
    private static final int ORACLE_PORT = 1521;

    @Bean(name = BEAN_NAME_EMBEDDED_ORACLEXE, destroyMethod = "stop")
    public ConcreteOracleContainer oracle(ConfigurableEnvironment environment,
                                          OracleXeProperties properties) {
        log.info("Starting oracle-xe server. Docker image: {}", properties.dockerImage);

        ConcreteOracleContainer oracle = (ConcreteOracleContainer) new ConcreteOracleContainer(properties.dockerImage)
                .withUsername(properties.getUser())
                .withPassword(properties.getPassword())
                .withLogConsumer(containerLogsConsumer(log))
                .withStartupTimeout(properties.getTimeoutDuration());
        oracle.start();
        registerOracleEnvironment(oracle, environment, properties);
        return oracle;
    }

    private void registerOracleEnvironment(ConcreteOracleContainer oracle,
                                           ConfigurableEnvironment environment,
                                           OracleXeProperties properties) {
        Integer mappedPort = oracle.getMappedPort(ORACLE_PORT);
        String host = oracle.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.oracle-xe.port", mappedPort);
        map.put("embedded.oracle-xe.host", host);
        map.put("embedded.oracle-xe.schema", properties.getDatabase());
        map.put("embedded.oracle-xe.user", properties.getUser());
        map.put("embedded.oracle-xe.password", properties.getPassword());

        String jdbcURL = "jdbc:oracle:thin://{}:{}/{}";
        log.info("Started oracle-xe server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedOracleXe", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    private static class ConcreteOracleContainer extends OracleContainer {
        public ConcreteOracleContainer(String dockerImageName) {
            super(dockerImageName);
        }
    }
}
