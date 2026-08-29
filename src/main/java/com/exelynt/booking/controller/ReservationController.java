package com.exelynt.booking.controller;

import com.exelynt.booking.dto.PagedResponse;
import com.exelynt.booking.dto.ReservationRequest;
import com.exelynt.booking.dto.ReservationResponse;
import com.exelynt.booking.dto.ReservationStatusUpdateRequest;
import com.exelynt.booking.entity.ReservationStatus;
import com.exelynt.booking.security.CustomUserDetails;
import com.exelynt.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservations", description = "Create and manage resource reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "List reservations with filtering, pagination and sorting. " +
            "ADMIN sees all reservations; USER sees only their own.")
    public ResponseEntity<PagedResponse<ReservationResponse>> getReservations(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(reservationService.getReservations(
                principal.getUser(), status, minPrice, maxPrice, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get a single reservation by id (owner or ADMIN only)")
    public ResponseEntity<ReservationResponse> getReservationById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(principal.getUser(), id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Create a reservation. The owner is always taken from the JWT, never from the request body.")
    public ResponseEntity<ReservationResponse> createReservation(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(201)
                .body(reservationService.createReservation(principal.getUser(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Update a reservation (owner while PENDING, or ADMIN at any time)")
    public ResponseEntity<ReservationResponse> updateReservation(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.updateReservation(principal.getUser(), id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Update reservation status. USER may only cancel their own pending reservation; " +
            "all other transitions are ADMIN-only.")
    public ResponseEntity<ReservationResponse> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody ReservationStatusUpdateRequest request) {
        return ResponseEntity.ok(reservationService.updateStatus(principal.getUser(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Delete a reservation (owner or ADMIN only)")
    public ResponseEntity<Void> deleteReservation(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        reservationService.deleteReservation(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }
}
