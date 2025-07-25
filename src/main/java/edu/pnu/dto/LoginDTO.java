package edu.pnu.dto;

import edu.pnu.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter 
@Setter 
@ToString 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class LoginDTO {
	private String userId;
	private String password;
	
	// id 중복 확인에 사용
	// Login은 JWT Filter에서 실행됨. (Service, Controller 없음)
	public static LoginDTO fromEntity(Member m) {
		return LoginDTO.builder()
				.userId(m.getUserId())
				.password(m.getPassword())
				.build();
	}
}
