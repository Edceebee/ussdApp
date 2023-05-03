package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

@Data
public class AppUserHandshakeResponse
{
    private boolean status;
    private String message;
    private AppUserHandshakeData data;
}
