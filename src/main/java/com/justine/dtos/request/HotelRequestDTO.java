package com.justine.dtos.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelRequestDTO {
    private String name;
    private String location;
    private String contactNumber;
    private String email;
}
