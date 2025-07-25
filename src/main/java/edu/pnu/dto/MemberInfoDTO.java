package edu.pnu.dto;

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
public class MemberInfoDTO {
	private String userId;
	private String userName;
	private String password;
	private String email;
	private String phone;
	private Long locationId;
	private Role role;
	
	public static MemberInfoDTO toEntity(Member m) {
		return MemberInfoDTO.builder()
				.userId(m.getUserId())
				.userName(m.getUserName())
				.password(m.getPassword())
				.email(m.getEmail())
				.phone(m.getPhone())
				.locationId(m.getLocation().getLocationId())
				.role(m.getRole())
				.build();
	}
}
