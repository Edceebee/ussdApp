package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

@Data
public class InternalNameEnquiryResponseDTO
{
    private String timeStamp;
    private boolean status;
    private String message;
    private InternalNameEnquiryData data;
}
