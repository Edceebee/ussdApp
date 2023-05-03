package com.backend.api.ussdservice.ussd_reflection.context.ussd;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class UssdPayload
{
    String input;
    String mobileNumber;
    String telco;
    String sessionId;
    String sessionType;
    String sessionOperation;
    String shortCodeString;
}
