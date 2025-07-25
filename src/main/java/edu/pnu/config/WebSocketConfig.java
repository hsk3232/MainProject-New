package edu.pnu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import edu.pnu.service.security.SecurityUserDetailsService;
import lombok.RequiredArgsConstructor;

//	■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
//	■■■■■■■■■■■■■■■■■■■■  Spring 서버가 WebSocket을 사용할 수 있게 설정 ■■■■■■■■■■■■■■■■■■■■■■
//	■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■

@RequiredArgsConstructor
@Configuration // 스프링 설정 파일임을 의미
@EnableWebSocketMessageBroker // 메시지 브로커(중계 서버)를 활성화 → Spring이 내부적으로 WebSocket 메시지를 pub/sub로 처리하게 함
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final SecurityUserDetailsService securityUserDetailsService;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// registerStompEnd=points 메서드
		// → 클라이언트(프론트, 브라우저, 앱 등)가 접속할 WebSocket 엔드포인트(주소) 등록
		
		System.out.println("[진입] : [WebSocketConfig] Websocket 설정 진입");
		
		registry.addEndpoint("/webSocket").addInterceptors(new JwtHandshakeInterceptor(securityUserDetailsService))
				.setAllowedOriginPatterns("http://localhost:3000") // CORS: 프론트엔드만 허용
				.withSockJS();
		
		System.out.println("[완료] : [WebSocketConfig] Websocket Front만 접근 가능 설정 완료");
	}
}


