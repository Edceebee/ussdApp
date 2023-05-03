package com.backend.api.ussdservice.ussd_reflection.context.exception;


import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;

public class InvalidShortCodeException extends RuntimeException
{
    private UssdContext ussdContext;
    public InvalidShortCodeException(){ super(); }
    public InvalidShortCodeException(String message){super(message);}
    public InvalidShortCodeException(String message, UssdContext ussdContext){
        super(message);
        this.ussdContext = ussdContext;
    }
    public UssdContext getUssdContext(){ return ussdContext; }
}
