package com.justine.dtos.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelRequestDTO {

    @NotBlank(message = "Hotel name is required")
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    private String contactNumber;

    @Email(message = "Invalid email format")
    private String email;

    /**
     * Existing image URL (kept when user doesn’t upload a new image)
     */
    private String hotelImageUrl;

    /**
     * New hotel image file for upload (only used in multipart requests)
     *
     * Must match the multipart field name in the frontend form.
     */
    private MultipartFile hotelImageFile;
}
