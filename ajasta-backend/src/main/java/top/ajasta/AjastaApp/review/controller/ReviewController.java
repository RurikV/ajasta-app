package top.ajasta.AjastaApp.review.controller;


import top.ajasta.AjastaApp.response.Response;
import top.ajasta.AjastaApp.review.dtos.ReviewDTO;
import top.ajasta.AjastaApp.review.services.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(name = "app.features.reviews", havingValue = "true", matchIfMissing = false)
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<ReviewDTO>> createReview(
            @RequestBody @Valid ReviewDTO reviewDTO
            ){
        return ResponseEntity.ok(reviewService.createReview(reviewDTO));
    }

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<Response<List<ReviewDTO>>> getReviewsForResource(
            @PathVariable Long resourceId) {
        return ResponseEntity.ok(reviewService.getReviewsForResource(resourceId));
    }

    @GetMapping("/resource/average/{resourceId}")
    public ResponseEntity<Response<Double>> getAverageRating(
            @PathVariable Long resourceId) {
        return ResponseEntity.ok(reviewService.getAverageRating(resourceId));
    }

    @GetMapping("/resource/eligibility/{resourceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<Boolean>> hasBookingForResource(
            @PathVariable Long resourceId) {
        return ResponseEntity.ok(reviewService.hasBookingForResource(resourceId));
    }

}
