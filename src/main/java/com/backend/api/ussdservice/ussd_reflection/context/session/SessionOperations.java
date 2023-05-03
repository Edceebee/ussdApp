package com.backend.api.ussdservice.ussd_reflection.context.session;

public enum SessionOperations
{
    BEGIN,
    CONTINUE,
    END;

    public String value(){
        return this.name().toLowerCase();
    }
}
