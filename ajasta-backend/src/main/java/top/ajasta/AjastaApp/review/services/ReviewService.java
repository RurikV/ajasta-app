package top.ajasta.AjastaApp.review.services;

import top.ajasta.AjastaApp.response.Response;
import top.ajasta.AjastaApp.review.dtos.ReviewDTO;

import java.util.List;

public interface ReviewService {
    Response<ReviewDTO> createReview(ReviewDTO reviewDTO);
    Response<List<ReviewDTO>> getReviewsForMenu(Long menuId);
    Response<Double> getAverageRating(Long menuId);
}
