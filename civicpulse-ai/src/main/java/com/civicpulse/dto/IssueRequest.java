package com.civicpulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IssueRequest {
    @NotBlank
    private String title;

    private String description;
    private String location;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
}
