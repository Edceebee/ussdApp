package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.ValidateBillDataParam;
import lombok.Data;

import java.util.List;

@Data
public class ValidateBillResponsePayload
{
    private String timeStamp;
    private boolean status;
    private String message;
    private List<ValidateBillDataParam> data;
}
