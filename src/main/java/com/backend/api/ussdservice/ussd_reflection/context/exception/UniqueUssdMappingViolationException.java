package com.backend.api.ussdservice.ussd_reflection.context.exception;

public class UniqueUssdMappingViolationException extends RuntimeException
{
    public UniqueUssdMappingViolationException(){ super(); }
    public UniqueUssdMappingViolationException(String message){ super(message); }
}
