package com.civicpulse.dto;

import com.civicpulse.model.Issue;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IssueResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String severity;
    private String location;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private int upvotes;
    private String status;
    private Long reportedById;
    private String reportedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static IssueResponse from(Issue issue) {
        return IssueResponse.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .category(issue.getCategory())
                .severity(issue.getSeverity())
                .location(issue.getLocation())
                .latitude(issue.getLatitude())
                .longitude(issue.getLongitude())
                .imageUrl(issue.getImageUrl())
                .upvotes(issue.getUpvotes())
                .status(issue.getStatus().name())
                .reportedById(issue.getReportedBy() == null ? null : issue.getReportedBy().getId())
                .reportedByName(issue.getReportedBy() == null ? null : issue.getReportedBy().getName())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }
}
