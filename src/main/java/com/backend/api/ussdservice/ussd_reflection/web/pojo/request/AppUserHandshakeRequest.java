package com.backend.api.ussdservice.ussd_reflection.web.pojo.request;

import lombok.Data;

@Data
public class AppUserHandshakeRequest
{
    private String emailOrPhoneNumber;
    private String otp;
    private String password;
}
