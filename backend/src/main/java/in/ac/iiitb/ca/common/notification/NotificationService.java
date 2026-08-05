package in.ac.iiitb.ca.common.notification;

import in.ac.iiitb.ca.common.error.ApiException;
import in.ac.iiitb.ca.common.tenant.TenantContext;
import in.ac.iiitb.ca.common.web.PageResponses;
import in.ac.iiitb.ca.security.SecurityUtils;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Service
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationService {

    public record NotificationResponse(
            UUID id,
            String title,
            String body,
            String link,
            Instant readAt,
            Instant createdAt
    ) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId(), n.getTitle(), n.getBody(), n.getLink(), n.getReadAt(), n.getCreatedAt());
        }
    }

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyUser(UUID userId, String title, String body, String link) {
        if (userId == null) {
            return;
        }
        Notification n = new Notification();
        n.setTenantId(TenantContext.getTenantId());
        n.setUserId(userId);
        n.setTitle(title);
        n.setBody(body);
        n.setLink(link);
        notificationRepository.save(n);
    }

    @Transactional
    public void notifyUsers(Iterable<UUID> userIds, String title, String body, String link) {
        if (userIds == null) {
            return;
        }
        for (UUID userId : userIds) {
            notifyUser(userId, title, body, link);
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PageResponses.PageResponse<NotificationResponse> myNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        UUID userId = SecurityUtils.currentUser().userId();
        Page<NotificationResponse> result = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageResponses.of(page, size, "createdAt", "desc"))
                .map(NotificationResponse::from);
        return PageResponses.from(result);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndReadAtIsNull(SecurityUtils.currentUser().userId());
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public NotificationResponse markRead(@PathVariable UUID id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUserId().equals(SecurityUtils.currentUser().userId())) {
            throw ApiException.forbidden("Access denied");
        }
        n.setReadAt(Instant.now());
        return NotificationResponse.from(notificationRepository.save(n));
    }
}
