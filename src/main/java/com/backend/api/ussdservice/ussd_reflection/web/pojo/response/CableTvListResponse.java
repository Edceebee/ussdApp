package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class CableTvListResponse
{
    private String timeStamp;
    private boolean status;
    private String message;
    List<CableTvResponseData> data;
}
