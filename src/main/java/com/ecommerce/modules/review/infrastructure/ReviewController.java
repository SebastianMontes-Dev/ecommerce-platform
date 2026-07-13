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
@RequestMapping("/api/v1/products/{idProducto}")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews and ratings")
public class ReviewController {

    private final ReviewRepository reviewRepository;

    @GetMapping("/reviews")
    @Operation(summary = "List product reviews")
    public ResponseEntity<?> listReviews(
            @PathVariable UUID idProducto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewRepository.findAllByIdProductoAndActivoTrue(idProducto, PageRequest.of(page, size)));
    }

    @PostMapping("/reviews")
    @Operation(summary = "Create a review")
    public ResponseEntity<Review> createReview(
            @PathVariable UUID idProducto, 
            @RequestBody CreateReviewRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.ecommerce.modules.identity.application.CustomUserDetails userDetails) {
        Review review = new Review();
        review.setIdTienda(TenantContext.getIdTienda());
        review.setIdProducto(idProducto);
        if (userDetails != null) {
            review.setCustomerId(userDetails.getUserId());
            review.setCustomerName(userDetails.getUsername());
        }
        review.setRating(Rating.of(request.getRating()));
        review.setTitulo(request.getTitulo());
        review.setComentario(request.getComentario());
        review = reviewRepository.save(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @lombok.Data
    static class CreateReviewRequest {
        private int rating;
        private String titulo;
        private String comentario;
    }
}
