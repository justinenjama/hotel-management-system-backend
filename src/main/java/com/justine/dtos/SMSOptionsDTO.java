package com.justine.wifi.dtos;

import lombok.Data;
import java.util.List;

@Data
public class SMSOptionsDTO {
    private List<String> to;
    private String message;
    private String from;
}

