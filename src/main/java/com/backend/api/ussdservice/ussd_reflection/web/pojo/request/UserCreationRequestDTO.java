package com.backend.api.ussdservice.ussd_reflection.web.pojo.request;

import lombok.Data;

@Data
public class UserCreationRequestDTO
{
    private boolean admin;
    private String email;
    private String firstName;
    private String password;
    private String phoneNumber;
    private String referenceCode = "";
    private String surname;
    private boolean ussd;
}
