package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

@Data
public class FundsTransferResponse
{
    private boolean cancelled;
    private boolean completedExceptionally;
    private boolean done;
    private Integer numberOfDependants;
    private String message;  // In case of system failure response.
}
