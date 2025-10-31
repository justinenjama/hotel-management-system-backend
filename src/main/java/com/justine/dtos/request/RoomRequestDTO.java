package com.justine.dtos.request;

import com.justine.enums.RoomType;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequestDTO {

    @NotBlank(message = "Room number is required")
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private RoomType type;

    @Positive(message = "Price per night must be positive")
    private Double pricePerNight;

    private boolean available = true;

    @NotNull(message = "Hotel ID is required")
    private Long hotelId;

    /**
     * Existing image URL (kept if no new file is uploaded)
     */
    private String roomImageUrl;

    /**
     * New room image file for upload
     */
    private MultipartFile roomImageFile;
}
