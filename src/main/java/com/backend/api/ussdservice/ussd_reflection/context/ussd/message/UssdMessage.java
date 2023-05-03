package com.backend.api.ussdservice.ussd_reflection.context.ussd.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class UssdMessage
{
    String message;                   // the message to the provider
}
