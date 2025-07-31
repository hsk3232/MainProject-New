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
public class MemberSettingDTO {
	private String userName;
	private String email;
	private String status;
	
	public static MemberSettingDTO fromEntity(Member m) {
		return MemberSettingDTO.builder()
				.userName(m.getUserName())
				.email(m.getEmail())
				.status(m.getStatus())
				.build();
	}
}
