package top.ajasta.AjastaApp.review.services;

import top.ajasta.AjastaApp.response.Response;
import top.ajasta.AjastaApp.review.dtos.ReviewDTO;

import java.util.List;

public interface ReviewService {
    Response<ReviewDTO> createReview(ReviewDTO reviewDTO);
    Response<List<ReviewDTO>> getReviewsForResource(Long resourceId);
    Response<Double> getAverageRating(Long resourceId);
    Response<Boolean> hasBookingForResource(Long resourceId);
}
