package com.backend.api.ussdservice.ussd_reflection.web.pojo.dto;

import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.UserInfoResponseData;
import lombok.Data;

@Data
public class UserInfoResponseDTO
{
    private boolean status;
    private String message;
    private long timestamp;
    private UserInfoResponseData data;
}
