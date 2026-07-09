package com.ecommerce.modules.review.infrastructure;

import com.ecommerce.modules.review.domain.*;
import com.ecommerce.modules.shared.domain.EntityNotFoundException;
import com.ecommerce.modules.shared.domain.Rating;
import com.ecommerce.modules.shared.infrastructure.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews and ratings")
public class ReviewController {

    private final ReviewRepository reviewRepository;

    @GetMapping("/reviews")
    @Operation(summary = "List product reviews")
    public ResponseEntity<?> listReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewRepository.findAllByProductIdAndActiveTrue(productId, PageRequest.of(page, size)));
    }

    @PostMapping("/reviews")
    @Operation(summary = "Create a review")
    public ResponseEntity<Review> createReview(@PathVariable UUID productId, @RequestBody CreateReviewRequest request) {
        Review review = new Review();
        review.setTenantId(TenantContext.getTenantId());
        review.setProductId(productId);
        review.setRating(Rating.of(request.getRating()));
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review = reviewRepository.save(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @lombok.Data
    static class CreateReviewRequest {
        private int rating;
        private String title;
        private String comment;
    }
}
