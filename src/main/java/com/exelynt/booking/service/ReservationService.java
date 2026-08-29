package com.exelynt.booking.service;

import com.exelynt.booking.dto.PagedResponse;
import com.exelynt.booking.dto.ReservationRequest;
import com.exelynt.booking.dto.ReservationResponse;
import com.exelynt.booking.dto.ReservationStatusUpdateRequest;
import com.exelynt.booking.entity.Reservation;
import com.exelynt.booking.entity.ReservationStatus;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.entity.Role;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.exception.BadRequestException;
import com.exelynt.booking.exception.ResourceNotFoundException;
import com.exelynt.booking.repository.ReservationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceService resourceService;

    public PagedResponse<ReservationResponse> getReservations(
            User currentUser,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Specification<Reservation> spec = buildSpecification(currentUser, status, minPrice, maxPrice);
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);

        Page<Reservation> resultPage = reservationRepository.findAll(spec, pageable);
        return toPagedResponse(resultPage);
    }

    public ReservationResponse getReservationById(User currentUser, Long id) {
        Reservation reservation = findReservationOrThrow(id);
        assertOwnerOrAdmin(currentUser, reservation);
        return toResponse(reservation);
    }

    public ReservationResponse createReservation(User currentUser, ReservationRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        Resource resource = resourceService.findResourceOrThrow(request.getResourceId());
        if (!resource.isAvailable()) {
            throw new BadRequestException("Resource is not available for booking");
        }

        BigDecimal price = request.getPrice() != null
                ? request.getPrice()
                : calculatePrice(resource, request);

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(currentUser)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ReservationStatus.PENDING)
                .price(price)
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse updateReservation(User currentUser, Long id, ReservationRequest request) {
        Reservation reservation = findReservationOrThrow(id);
        assertOwnerOrAdmin(currentUser, reservation);

        if (currentUser.getRole() == Role.USER && reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BadRequestException("Only pending reservations can be modified");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        Resource resource = resourceService.findResourceOrThrow(request.getResourceId());

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());

        if (currentUser.getRole() == Role.ADMIN && request.getPrice() != null) {
            reservation.setPrice(request.getPrice());
        } else if (currentUser.getRole() == Role.USER) {
            reservation.setPrice(calculatePrice(resource, request));
        }

        return toResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse updateStatus(User currentUser, Long id, ReservationStatusUpdateRequest request) {
        Reservation reservation = findReservationOrThrow(id);

        boolean isOwner = reservation.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isAdmin) {
            if (!isOwner) {
                throw new AccessDeniedException("You do not have permission to update this reservation");
            }
            if (request.getStatus() != ReservationStatus.CANCELLED) {
                throw new AccessDeniedException("Only administrators can set this status");
            }
            if (reservation.getStatus() != ReservationStatus.PENDING) {
                throw new BadRequestException("Only pending reservations can be cancelled");
            }
        }

        reservation.setStatus(request.getStatus());
        return toResponse(reservationRepository.save(reservation));
    }

    public void deleteReservation(User currentUser, Long id) {
        Reservation reservation = findReservationOrThrow(id);
        assertOwnerOrAdmin(currentUser, reservation);
        reservationRepository.delete(reservation);
    }

    private BigDecimal calculatePrice(Resource resource, ReservationRequest request) {
        if (resource.getPricePerHour() == null) {
            return BigDecimal.ZERO;
        }
        long minutes = java.time.Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, java.math.RoundingMode.HALF_UP);
        return resource.getPricePerHour().multiply(hours).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void assertOwnerOrAdmin(User currentUser, Reservation reservation) {
        boolean isOwner = reservation.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to access this reservation");
        }
    }

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private Specification<Reservation> buildSpecification(
            User currentUser, ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (currentUser.getRole() == Role.USER) {
                predicates.add(cb.equal(root.get("user").get("id"), currentUser.getId()));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        List<String> allowedSortFields = List.of("id", "startTime", "endTime", "price", "status", "createdAt");
        String property = (sortBy != null && allowedSortFields.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, property));
    }

    private PagedResponse<ReservationResponse> toPagedResponse(Page<Reservation> resultPage) {
        return PagedResponse.<ReservationResponse>builder()
                .content(resultPage.getContent().stream().map(this::toResponse).toList())
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .last(resultPage.isLast())
                .build();
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .resourceId(reservation.getResource().getId())
                .resourceName(reservation.getResource().getName())
                .userId(reservation.getUser().getId())
                .username(reservation.getUser().getUsername())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(reservation.getStatus())
                .price(reservation.getPrice())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
