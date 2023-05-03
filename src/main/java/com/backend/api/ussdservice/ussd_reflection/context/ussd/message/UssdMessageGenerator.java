package com.backend.api.ussdservice.ussd_reflection.context.ussd.message;

import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;

public class UssdMessageGenerator
{
    public static UssdMessage generateRequiredInputMessage(MenuOptionBuilder builder, UssdContext context){
        return generateRequiredInputMessage(builder.toString(), context);
    }
    public static UssdMessage generateRequiredInputMessage(String message, UssdContext context){
        UssdMessage halloTagMessage = new UssdMessage();
        halloTagMessage.setMessage(message);
        return halloTagMessage;
    }

    public static UssdMessage generateViewOnlyMessageForSessionContinuation(String message, UssdContext context){
        UssdMessage halloTagMessage = new UssdMessage();
        halloTagMessage.setMessage(message);
        return halloTagMessage;
    }

    public static UssdMessage generateViewOnlyMessageForSessionAbortion(String message, UssdContext context){
        UssdMessage halloTagMessage = new UssdMessage();
        halloTagMessage.setMessage(message);
        return halloTagMessage;
    }


}
