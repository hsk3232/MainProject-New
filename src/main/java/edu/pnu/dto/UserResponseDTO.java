package edu.pnu.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private String role;        // "UNAUTH", "MANAGER" 등
    private Long locationId;
    private String userId;
    private String userName;
    private String email;
    private String status;      // "pending", "active", "rejected", "inactive", "del"
    private LocalDateTime createdAt;
}