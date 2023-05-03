package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class PackageResponseData
{
    private String categoryId;
    private String billerId;
    private List<PackageItem> items;
}
