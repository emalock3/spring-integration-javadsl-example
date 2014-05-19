package com.github.emalock3.spring;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.messaging.MessageChannel;

@Configuration
@ComponentScan
@IntegrationComponentScan
@EnableAutoConfiguration
public class Application {
    
    private static final AtomicReference<ConfigurableApplicationContext> applicationContextRef = 
            new AtomicReference<>();
    
    public static void main(String ... args) throws IOException {
        applicationContextRef.set(new SpringApplicationBuilder(Application.class)
                .showBanner(false)
                .run(args));
    }
    
    @Bean
    TcpNetServerConnectionFactory tcpGateConnFactory() {
        return new TcpNetServerConnectionFactory(9876);
    }
    
    @Bean
    TcpInboundGateway tcpGate() {
        TcpInboundGateway gate = new TcpInboundGateway();
        gate.setConnectionFactory(tcpGateConnFactory());
        gate.setRequestChannel(requestGreetChannel());
        return gate;
    }
    
    @Bean
    public MessageChannel requestGreetChannel() {
        return new DirectChannel();
    }
    
    @Bean
    public IntegrationFlow greetingFlow() {
        return IntegrationFlows.from(requestGreetChannel())
                .transform(new ObjectToStringTransformer())
                .transform("'Hello, ' + payload + '!'")
                .get();
    }
    
    @Bean
    TcpNetServerConnectionFactory shutdownConnFactory() {
        return new TcpNetServerConnectionFactory(19876);
    }
    
    @Bean
    TcpInboundGateway shutdownGate() {
        TcpInboundGateway gate = new TcpInboundGateway();
        gate.setConnectionFactory(shutdownConnFactory());
        gate.setRequestChannel(requestShutdownChannel());
        return gate;
    }
    
    @Bean
    public MessageChannel requestShutdownChannel() {
        return new DirectChannel();
    }
    
    @Bean
    public IntegrationFlow shutdownFlow() {
        return IntegrationFlows.from(requestShutdownChannel())
                .handle(message -> shutdownContext(applicationContextRef.get()))
                .get();
    }
    
    private void shutdownContext(ConfigurableApplicationContext context) {
        try {
            context.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, DisposableBean.class).destroy();
        } catch (Exception ignore) {
        }
        context.stop();
    }
    
}
