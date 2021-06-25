/*
 * The MIT License
 *
 * Copyright 2020 randalkamradt.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.kamradtfamily.blockchainnode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import java.util.Properties;

/**
 *
 * @author randalkamradt
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackageClasses = {
        net.kamradtfamily.blockchain.api.Blockchain.class,
        net.kamradtfamily.blockchainnode.Application.class})
@EnableReactiveMongoRepositories(basePackageClasses = {
        net.kamradtfamily.blockchain.api.BlockRepository.class,
        net.kamradtfamily.blockchainnode.NodeRepository.class})
public class Application {
    public static void main(String [] args) {
        SpringApplication.run(Application.class, args);
        try {
            Properties gitProps = new Properties();
            gitProps.load(Application.class.getResourceAsStream("/git.properties"));
            log.info("Git Properties:");
            gitProps.entrySet().stream()
                    .forEach(es -> log.info("{}: {}", es.getKey(),es.getValue()));
        } catch (Exception e) {
            log.error("Error reading Git Properties");
        }
    }
}
