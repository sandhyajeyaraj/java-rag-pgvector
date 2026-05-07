package com.complianceiq.v1.repository;

import com.complianceiq.v1.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findByCategory(String category);

    List<Policy> findByStatus(String status);

    @Query("SELECT p FROM Policy p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Policy> searchByKeyword(String keyword);
}
