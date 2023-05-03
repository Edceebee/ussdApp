package com.backend.api.ussdservice.ussd_reflection.web.pojo;

import lombok.Data;

@Data
public class WayaPayWebResponse
{
    private Long timestamp;
    private boolean status;
    private String message;
    private Object data;
}
