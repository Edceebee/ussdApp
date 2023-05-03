package com.backend.api.ussdservice.ussd_reflection.web.pojo.request;

import lombok.Data;

import java.util.List;

@Data
public class CableTvRequest
{
    private double amount;
    private String billerId;
    private String categoryId;
    private List<CableTvRequestData> data;
    private String sourceWalletAccountNumber;
}
