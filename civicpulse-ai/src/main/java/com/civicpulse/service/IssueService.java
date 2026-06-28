package com.civicpulse.service;

import com.civicpulse.dto.IssueRequest;
import com.civicpulse.model.Issue;
import com.civicpulse.model.User;
import com.civicpulse.repository.IssueRepository;
import com.civicpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

    public Issue createIssue(IssueRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        Issue issue = new Issue();
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setLocation(request.getLocation());
        issue.setLatitude(request.getLatitude());
        issue.setLongitude(request.getLongitude());
        issue.setImageUrl(request.getImageUrl());
        issue.setReportedBy(user);

        // AI categorization via Gemini
        Map<String, String> analysis = geminiService.analyzeIssue(
                request.getTitle(), request.getDescription());
        issue.setCategory(analysis.get("category"));
        issue.setSeverity(analysis.get("severity"));

        return issueRepository.save(issue);
    }

    public List<Issue> getAllIssues() {
        return issueRepository.findAll();
    }

    public Issue getIssueById(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found!"));
    }

    public List<Issue> getIssuesByStatus(String status) {
        return issueRepository.findByStatus(
                Issue.Status.valueOf(status.toUpperCase()));
    }

    public List<Issue> getIssuesByLocation(String location) {
        return issueRepository.findByLocationContainingIgnoreCase(location);
    }

    public List<Issue> getIssuesByCategory(String category) {
        return issueRepository.findByCategory(category.toUpperCase());
    }

    public Issue updateIssueStatus(Long id, String status) {
        Issue issue = getIssueById(id);
        issue.setStatus(Issue.Status.valueOf(status.toUpperCase()));
        issue.setUpdatedAt(LocalDateTime.now());
        return issueRepository.save(issue);
    }

    public Issue upvoteIssue(Long id) {
        Issue issue = getIssueById(id);
        issue.setUpvotes(issue.getUpvotes() + 1);
        return issueRepository.save(issue);
    }

    public List<Issue> getUserIssues(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        return issueRepository.findByReportedById(user.getId());
    }

    public String generateSummary(List<Issue> issues) {
        if (issues.isEmpty()) return "No issues reported yet.";

        long open = issues.stream()
                .filter(i -> i.getStatus() == Issue.Status.OPEN).count();
        long inProgress = issues.stream()
                .filter(i -> i.getStatus() == Issue.Status.IN_PROGRESS).count();
        long resolved = issues.stream()
                .filter(i -> i.getStatus() == Issue.Status.RESOLVED).count();
        long critical = issues.stream()
                .filter(i -> "CRITICAL".equals(i.getSeverity())).count();
        long high = issues.stream()
                .filter(i -> "HIGH".equals(i.getSeverity())).count();

        String issueList = issues.stream()
                .map(i -> "- " + i.getTitle() + " at " + i.getLocation()
                        + " (Status: " + i.getStatus()
                        + ", Severity: " + i.getSeverity() + ")")
                .collect(java.util.stream.Collectors.joining("\n"));

        return geminiService.generateCitySummary(
                issues.size(), open, inProgress, resolved,
                critical, high, issueList);
    }
}