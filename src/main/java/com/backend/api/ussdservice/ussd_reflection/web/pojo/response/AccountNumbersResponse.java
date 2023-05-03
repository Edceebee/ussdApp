package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

@Data
public class AccountNumbersResponse
{
    private Long id;
    private String accountNo;
    private String nubanAccountNo;
}
