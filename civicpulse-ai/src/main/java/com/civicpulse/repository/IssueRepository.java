package com.civicpulse.repository;

import com.civicpulse.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByStatus(Issue.Status status);
    List<Issue> findByCategory(String category);
    List<Issue> findByReportedById(Long userId);
    List<Issue> findByLocationContainingIgnoreCase(String location);
}