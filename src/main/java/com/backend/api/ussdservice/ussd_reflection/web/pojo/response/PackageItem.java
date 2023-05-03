package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class PackageItem
{
    private String paramName;
    private boolean isAmountFixed;
    private List<PackageSubItem> subItems;
}
