package com.hotelmanager.config;

import com.hotelmanager.domain.AuditEvent;
import com.hotelmanager.domain.User;
import com.hotelmanager.repository.AuditEventRepository;
import com.hotelmanager.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final SecurityUtils securityUtils;

    public AuditService(AuditEventRepository auditEventRepository, SecurityUtils securityUtils) {
        this.auditEventRepository = auditEventRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, Long entityId, Map<String, Object> metadata) {
        AuditEvent event = new AuditEvent();
        User user = securityUtils.getCurrentUser().orElse(null);
        event.setUser(user);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        Map<String, Object> meta = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        event.setMetadata(meta);
        auditEventRepository.save(event);
    }

    public void record(String action, String entityType, Long entityId) {
        record(action, entityType, entityId, null);
    }
}
