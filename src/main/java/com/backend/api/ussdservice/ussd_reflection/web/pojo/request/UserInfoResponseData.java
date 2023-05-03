package com.backend.api.ussdservice.ussd_reflection.web.pojo.request;

import lombok.Data;

@Data
public class UserInfoResponseData
{
    private String firstName;
    private String middleName;
    private String lastName;
    private long userId;
}
