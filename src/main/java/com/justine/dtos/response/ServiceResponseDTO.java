package com.justine.dtos.response;

import com.justine.enums.ServiceType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponseDTO {
    private Long id;
    private ServiceType serviceType;
    private Double price;
}
