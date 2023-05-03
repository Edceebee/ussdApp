package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

@Data
public class WayaPayUserWebResponse
{
    private Long timestamp;
    private boolean status;
    private String message;
    private UserProfileDetailsData data;
}
