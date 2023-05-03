package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WayaGenericError extends ValidateBillResponsePayload
{
    private String timeStamp;
    private boolean status;
    private String message;
}
