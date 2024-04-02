package com.example.app.functional.router;

import com.example.app.functional.handler.GreetingHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

@Configuration(proxyBeanMethods = false)
public class GreetingRouter {

    @Bean
    public RouterFunction<ServerResponse> route(GreetingHandler greetingHandler) {
        final RequestPredicate helloRoute = RequestPredicates
                .GET("/hello")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        final RequestPredicate mainRoute = RequestPredicates
                .GET("/");

        return RouterFunctions
                .route(helloRoute, greetingHandler::hello)
                .andRoute(mainRoute, greetingHandler::render);
    }
}
