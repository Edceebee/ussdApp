package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

@Data
public class PackageSubItem
{
    private String id;
    private String name;
    private String minAmount;
    private String amount;
}
