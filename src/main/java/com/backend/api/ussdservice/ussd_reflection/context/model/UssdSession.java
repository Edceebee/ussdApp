package com.backend.api.ussdservice.ussd_reflection.context.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
public class UssdSession
{
    private Long id;

    private String sessionId;

    private String contextData;

    private String mobileNumber;

    private String telco;

    private Date sessionStartDate;

    private Date sessionEndDate;

    private boolean isShortCodeStarted = false;

    public Object extraData;
}
