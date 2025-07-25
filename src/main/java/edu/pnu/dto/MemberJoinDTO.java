package edu.pnu.dto;

import edu.pnu.domain.Location;
import edu.pnu.domain.Member;
import edu.pnu.domain.Role;
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
public class MemberJoinDTO {
	private String userId;
	private String userName;
	private String password;
	private String email;
	private String phone;
	private Long locationId;
	private Role role;
	
	//entity에 저장하는 Method
	public Member toEntity() {
		 // DTO의 locationId로 Location 엔티티의 PK만 가진 객체를 임시로 생성
	    Location location = Location.builder()
	        .locationId(this.locationId)
	        .build();

	    return Member.builder()
	            .userId(userId)
	            .userName(userName)
	            .password(password)
	            .email(email)
	            .phone(phone)
	            .role(role != null ? role : Role.ROLE_UNAUTH)
	            .location(location) // ⭐️ locationId(Long) → Location 객체
	            .build();
	}
}
