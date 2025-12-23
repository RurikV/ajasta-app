package top.ajasta.AjastaApp.review.services;


import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.auth_users.services.UserService;
import top.ajasta.AjastaApp.exceptions.BadRequestException;
import top.ajasta.AjastaApp.exceptions.NotFoundException;
import top.ajasta.AjastaApp.order.repository.OrderRepository;
import top.ajasta.AjastaApp.reservation.entity.Resource;
import top.ajasta.AjastaApp.reservation.repository.ResourceRepository;
import top.ajasta.AjastaApp.response.Response;
import top.ajasta.AjastaApp.review.dtos.ReviewDTO;
import top.ajasta.AjastaApp.review.entity.Review;
import top.ajasta.AjastaApp.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ResourceRepository resourceRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final OrderRepository orderRepository;


    @Override
    @Transactional
    public Response<ReviewDTO> createReview(ReviewDTO reviewDTO) {

        log.info("Inside createReview()");

        // Get current user
        User user = userService.getCurrentLoggedInUser();

        // Validate required fields
        if (reviewDTO.getResourceId() == null) {
            throw new BadRequestException("Resource ID is required");
        }

        // Validate resource exists
        Resource resource = resourceRepository.findById(reviewDTO.getResourceId())
                .orElseThrow(() -> new NotFoundException("Resource not found"));

        // Ensure the user has booked this resource
        boolean eligible = orderRepository.userHasBookingWithTitleLike(user.getId(), resource.getName());
        if (!eligible) {
            throw new BadRequestException("You can only review resources you've booked");
        }

        // Optional: prevent duplicate reviews per order if provided
        if (reviewDTO.getOrderId() != null) {
            boolean exists = reviewRepository.existsByUserIdAndResourceIdAndOrderId(
                    user.getId(), reviewDTO.getResourceId(), reviewDTO.getOrderId());
            if (exists) {
                throw new BadRequestException("You've already reviewed this resource for this order");
            }
        }
        // Prevent duplicate review for the same resource by same user
        boolean alreadyReviewed = reviewRepository.existsByUserIdAndResourceId(user.getId(), resource.getId());
        if (alreadyReviewed) {
            throw new BadRequestException("You've already reviewed this resource");
        }

        // Create and save review
        Review review = Review.builder()
                .user(user)
                .resource(resource)
                .orderId(reviewDTO.getOrderId())
                .rating(reviewDTO.getRating())
                .comment(reviewDTO.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);

        // Return response with review data
        ReviewDTO responseDto = modelMapper.map(savedReview, ReviewDTO.class);
        responseDto.setUserName(user.getName());
        responseDto.setResourceName(resource.getName());

        return Response.<ReviewDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Review added successfully")
                .data(responseDto)
                .build();

    }

    @Override
    public Response<List<ReviewDTO>> getReviewsForResource(Long resourceId) {
        log.info("Inside getReviewsForResource()");

        List<Review> reviews = reviewRepository.findByResourceIdOrderByIdDesc(resourceId);

        List<ReviewDTO> reviewDTOs = reviews.stream()
                .map(review -> {
                    ReviewDTO dto = modelMapper.map(review, ReviewDTO.class);
                    dto.setUserName(review.getUser() != null ? review.getUser().getName() : null);
                    dto.setResourceName(review.getResource() != null ? review.getResource().getName() : null);
                    return dto;
                })
                .toList();

        return Response.<List<ReviewDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Reviews retrieved successfully")
                .data(reviewDTOs)
                .build();

    }

    @Override
    public Response<Double> getAverageRating(Long resourceId) {
        log.info("Inside getAverageRating()");

        Double averageRating = reviewRepository.calculateAverageRatingByResourceId(resourceId);

        return Response.<Double>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Average rating retrieved successfully")
                .data(averageRating != null ? averageRating : 0.0)
                .build();
    }

    @Override
    public Response<Boolean> hasBookingForResource(Long resourceId) {
        log.info("Inside hasBookingForResource()");
        User user = userService.getCurrentLoggedInUser();
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found"));
        boolean eligible = orderRepository.userHasBookingWithTitleLike(user.getId(), resource.getName());
        return Response.<Boolean>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Eligibility retrieved successfully")
                .data(eligible)
                .build();
    }
}
