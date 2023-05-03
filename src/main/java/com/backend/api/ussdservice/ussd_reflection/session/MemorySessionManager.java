package com.backend.api.ussdservice.ussd_reflection.session;


import com.backend.api.ussdservice.ussd_reflection.context.model.UssdSession;

import java.util.HashMap;
import java.util.Map;

public class MemorySessionManager
{
    /**
     * An in memory map that is used to store a session for a user. The map relates a sessionId to any
     * object that is desired to be stored for the user menu continuous interaction.
     */
    private static final Map<String, UssdSession> memorySessionStorage = new HashMap<>();

    UssdSession findBySessionId(String sessionId){
        return memorySessionStorage.get(sessionId);
    }

    UssdSession save(UssdSession ussdSession){
        memorySessionStorage.put(ussdSession.getSessionId(), ussdSession);
        return ussdSession;
    }

    UssdSession remove(String sessionId){
        return memorySessionStorage.remove(sessionId);
    }
    public <T> T getExtraData(String sessionId, Class<T> clazz){
        return (T) this.findBySessionId(sessionId).getExtraData();
    }

    public Object getExtraData(String sessionId){
        return this.findBySessionId(sessionId).getExtraData();
    }
    public void updateExtraData(String sessionId, Object data){
        this.findBySessionId(sessionId).setExtraData(data);
    }
}
