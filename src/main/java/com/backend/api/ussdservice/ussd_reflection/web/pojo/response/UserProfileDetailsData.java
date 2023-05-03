package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

@Data
public class UserProfileDetailsData
{
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String middleName;
}
