package com.complianceiq.v2.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryAuditLogRepository extends JpaRepository<QueryAuditLog, Long> {}
