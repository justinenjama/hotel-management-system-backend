package com.justine.dtos.request;

import com.justine.enums.ServiceType;
import lombok.Data;

@Data
public class CreateServiceRequest {
    private ServiceType serviceType;
    private String name;
    private String description;
    private Double price;
}

