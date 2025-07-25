package edu.pnu.config;

import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import edu.pnu.service.security.SecurityUserDetailsService;
import lombok.RequiredArgsConstructor;



@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
	
	private final SecurityUserDetailsService userDetailsService;
	
	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws Exception {
		
		System.out.println("[진입] : [JwtHandshakeInterceptor] WebSocket 사용을 위한 인증 객체 진입 ");
		
		   // 1. 헤더에서 JWT 추출
	    List<String> authHeaders = request.getHeaders().get("Authorization");
	    if (authHeaders != null && !authHeaders.isEmpty()) {
	        String token = authHeaders.get(0).replace("Bearer ", "");
	        
	        String userId = JWT.require(Algorithm.HMAC256("edu.pnu.jwt"))
                    .build()
                    .verify(token)
                    .getClaim("userId")
                    .asString();
	        
	        // 2. 내 서비스로 유저 정보 조회
	        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(userId);
	        // 3. attributes에 저장
	        attributes.put("userDetails", userDetails);
	        
	        System.out.println("[성공] : [JwtHandshakeInterceptor] WebSocket userDetails 정보 저장 성공 ");
	        return true;
	    } else {
	    	System.err.println("[오류] : [JwtHandshakeInterceptor] 인증 객체 없음");
	        return false; // 인증 없으면 거부
	    }
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {
		// TODO Auto-generated method stub

	}

}
