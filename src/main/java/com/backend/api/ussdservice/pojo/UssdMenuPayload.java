package com.backend.api.ussdservice.pojo;

import lombok.Data;

@Data
public class UssdMenuPayload
{
    private String sessionId;
    private String serviceCode;
    private String phoneNumber;
    private String text;
    private String networkCode;
}
