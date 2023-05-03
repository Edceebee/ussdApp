package com.backend.api.ussdservice.ussd_reflection.web.pojo.request;

import lombok.Data;

@Data
public class UserEmailOrPhoneVerifyRequestDTO
{
    private String otp;
    private String phoneOrEmail;
}
