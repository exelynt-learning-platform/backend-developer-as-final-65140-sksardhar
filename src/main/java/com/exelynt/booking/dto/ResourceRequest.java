package com.exelynt.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Type is required")
    private String type;

    private String location;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price per hour must not be negative")
    private BigDecimal pricePerHour;

    private Boolean available;
}
