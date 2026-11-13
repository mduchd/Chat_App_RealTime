package com.teamchat.realtime_chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import java.security.Principal;
import java.util.Map;

// Các thư viện import thêm cho chốt chặn bảo mật 
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.MessageHeaderAccessor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${chat.demo-identities:false}")
    private boolean demoIdentities;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        DefaultHandshakeHandler handler = new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                    Map<String, Object> attributes) {
                Principal authenticated = request.getPrincipal();
                if (authenticated != null || !demoIdentities) return authenticated;
                String username = UriComponentsBuilder.fromUri(request.getURI()).build()
                        .getQueryParams().getFirst("user");
                if (username == null || !username.matches("[A-Za-z0-9_-]{1,32}")) return null;
                return () -> username;
            }
        };
        registry.addEndpoint("/ws")
                .setHandshakeHandler(handler)
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws-sockjs")
                .setHandshakeHandler(handler)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    //  Kiểm tra quyền và chặn đứng truy cập trái phép
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                // Khi có yêu cầu bắt đầu kết nối (CONNECT)
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Principal user = accessor.getUser();
                    
                    // Nếu danh tính bị null (chưa đăng nhập và không dùng demo) -> Đuổi ra ngoài
                    if (user == null) {
                        System.err.println("Từ chối truy cập WebSocket: Người dùng chưa xác thực!");
                        throw new IllegalArgumentException("Từ chối truy cập: Bạn chưa đăng nhập!");
                    }
                }
                return message;
            }
        });
    }
}
