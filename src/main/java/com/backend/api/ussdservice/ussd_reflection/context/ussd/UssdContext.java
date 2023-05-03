package com.backend.api.ussdservice.ussd_reflection.context.ussd;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UssdContext
{
    String input;
    String mobileNumber;
    String telco;
    String sessionId;
    String sessionType;
    String sessionOperation;
    String originatingContextData;
}
