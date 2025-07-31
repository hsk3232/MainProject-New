package edu.pnu.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.dto.UserFactoryUpdateDTO;
import edu.pnu.dto.UserResponseDTO;
import edu.pnu.dto.UserStatusUpdateDTO;
import edu.pnu.service.AdminUserService;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminUserController {
	
	private final AdminUserService adminUserService;

    // 사용자 목록 조회
    @GetMapping("/users")
    public ResponseEntity<?> getUserList() {
        List<UserResponseDTO> users = adminUserService.getUserList();
        return ResponseEntity.ok(Map.of("users", users));
    }

    // 사용자 상태 변경
    @PatchMapping("/users/status")
    public ResponseEntity<?> updateUserStatus(@RequestBody UserStatusUpdateDTO dto) {
        adminUserService.updateUserStatus(dto);
        return ResponseEntity.ok().body("상태가 변경되었습니다.");
    }
    
    @PatchMapping("/users/factory")
    public ResponseEntity<?> updateUserFactory(@RequestBody UserFactoryUpdateDTO dto) {
        log.info("[진입] : [AdminUserController] 소속 공장 변경 - userId: {}, locationId: {}", dto.getUserId(), dto.getLocationId());
        if (dto.getUserId() == null || dto.getLocationId() == null) {
            log.error("DTO 값이 제대로 전달되지 않았습니다. DTO: {}", dto);
            return ResponseEntity.badRequest().body("userId 또는 locationId 값이 누락되었습니다.");
        }
        adminUserService.updateUserFactory(dto);
        return ResponseEntity.ok("소속 공장이 변경되었습니다.");
    }
}
