package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

@Data
public class PackageListResponse
{
    private String timeStamp;
    private boolean status;
    private String message;
    private PackageResponseData data;
}
