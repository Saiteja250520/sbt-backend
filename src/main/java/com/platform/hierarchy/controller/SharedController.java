package com.platform.hierarchy.controller;

import com.platform.hierarchy.model.AuditLog;
import com.platform.hierarchy.model.Notification;
import com.platform.hierarchy.model.PlatformSettings;
import com.platform.hierarchy.model.User;
import com.platform.hierarchy.repository.PlatformSettingsRepository;
import com.platform.hierarchy.repository.UserRepository;
import com.platform.hierarchy.service.AuditLogService;
import com.platform.hierarchy.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shared")
public class SharedController {

    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final PlatformSettingsRepository platformSettingsRepository;

    public SharedController(NotificationService notificationService, AuditLogService auditLogService,
                            UserRepository userRepository, PlatformSettingsRepository platformSettingsRepository) {
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.platformSettingsRepository = platformSettingsRepository;
    }

    private User getUser(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications(Principal principal) {
        User user = getUser(principal.getName());
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Map<String, String>> markNotificationAsRead(@PathVariable Long id, Principal principal) {
        try {
            User user = getUser(principal.getName());
            notificationService.markAsRead(id, user);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Notification marked as read.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<PlatformSettings> getPlatformSettings() {
        PlatformSettings settings = platformSettingsRepository.findById(1L)
                .orElseGet(() -> platformSettingsRepository.save(new PlatformSettings()));
        return ResponseEntity.ok(settings);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('SYSTEM_OWNER')")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }
}
