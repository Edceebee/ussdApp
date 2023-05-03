package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

@Data
public class AccountBalanceResponseData
{
    private String accountNo;
    private String accountName;
    private double balance;
}
