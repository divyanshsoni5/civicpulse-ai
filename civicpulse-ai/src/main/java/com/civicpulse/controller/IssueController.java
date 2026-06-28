package com.civicpulse.controller;

import com.civicpulse.dto.IssueRequest;
import com.civicpulse.model.Issue;
import com.civicpulse.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    public ResponseEntity<Issue> createIssue(
            @RequestBody IssueRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                issueService.createIssue(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<Issue>> getAllIssues() {
        return ResponseEntity.ok(issueService.getAllIssues());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Issue> getIssueById(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.getIssueById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Issue>> getByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(issueService.getIssuesByStatus(status));
    }

    @GetMapping("/location")
    public ResponseEntity<List<Issue>> getByLocation(
            @RequestParam String q) {
        return ResponseEntity.ok(issueService.getIssuesByLocation(q));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Issue>> getByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(issueService.getIssuesByCategory(category));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Issue> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(issueService.updateIssueStatus(id, status));
    }

    @PutMapping("/{id}/upvote")
    public ResponseEntity<Issue> upvoteIssue(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.upvoteIssue(id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Issue>> getMyIssues(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                issueService.getUserIssues(userDetails.getUsername()));
    }
}