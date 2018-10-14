package com.neo.fallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 请求路径 http://127.0.0.1:8888/spring-cloud-producer/hello?name=zskx&token=4
 * 向用户管理spring-cloud-producer路由发起请求失败时的回滚处理
 * hystrix的回滚能力
 */
@Component
public class ProducerFallback implements FallbackProvider {
    private final Logger logger = LoggerFactory.getLogger(FallbackProvider.class);

    //指定要处理的 service。
    //api服务id，如果需要所有调用都支持回退，则return "*"或return null
    @Override
    public String getRoute() {
        return "spring-cloud-producer";
    }

    /**
     * 如果请求用户服务失败，返回什么信息给消费者客户端
     */
    public ClientHttpResponse fallbackResponse() {
        return new ClientHttpResponse() {

            /**
             * 网关向api服务请求是失败了，但是消费者客户端向网关发起的请求是OK的，
             * 不应该把api的404,500等问题抛给客户端
             * 网关和api服务集群对于客户端来说是黑盒子
             */
            @Override
            public HttpStatus getStatusCode() throws IOException {
                System.out.println("===> ProducerFallback.getStatusCode()");
                return HttpStatus.OK;
            }

            @Override
            public int getRawStatusCode() throws IOException {
                System.out.println("===> ProducerFallback.getRawStatusCode()");
                return 200;
            }

            @Override
            public String getStatusText() throws IOException {
                System.out.println("===> ProducerFallback.getStatusText()");
                return "OK";
            }

            @Override
            public void close() {
                System.out.println("===> ProducerFallback.close()");
            }


            /**
             * 当 spring-cloud-producer 微服务出现宕机后，客户端再请求时候就会返回 fallback 等字样的字符串提示；
             *
             * 但对于复杂一点的微服务，我们这里就得好好琢磨该怎么友好提示给用户了；
             *
             * 如果请求用户服务失败，返回什么信息给消费者客户端
             */
            @Override
            public InputStream getBody() throws IOException {
                System.out.println("===> ProducerFallback.getBody()");
                return new ByteArrayInputStream("The service is unavailable.".getBytes());
            }

            //和body中的内容编码一致，否则容易乱码
            @Override
            public HttpHeaders getHeaders() {
                System.out.println("===> ProducerFallback.getHeaders()");
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
            }
        };
    }

    @Override
    public ClientHttpResponse fallbackResponse(Throwable cause) {
        System.out.println("===> ProducerFallback.fallbackResponse()");
        if (cause != null && cause.getCause() != null) {
            String reason = cause.getCause().getMessage();
            logger.info("Excption {}",reason);
        }
        return fallbackResponse();
    }
}
