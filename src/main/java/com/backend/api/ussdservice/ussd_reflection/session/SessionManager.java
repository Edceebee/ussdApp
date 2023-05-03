package com.backend.api.ussdservice.ussd_reflection.session;

import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.Item;
import com.backend.api.ussdservice.ussd_reflection.context.model.UssdSession;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionManager
{
    private static final MemorySessionManager memorySessionRepository = new MemorySessionManager();

    public UssdSession save(UssdSession ussdSession){
        return memorySessionRepository.save(ussdSession);
    }

    public UssdSession findBySessionId(String sessionId){
        return memorySessionRepository.findBySessionId(sessionId);
    }

    public static UssdSession clearSession(String sessionId){
        return memorySessionRepository.remove(sessionId);
    }


    public static <T> T getExtraDataOfSession(String sessionId, Class<T> clazz){
        return (T) memorySessionRepository.getExtraData(sessionId, clazz);
    }

    public static Object getExtraDataOfSession(String sessionId){
        return memorySessionRepository.getExtraData(sessionId);
    }

    public static void updateExtraDataOfSession(String sessionId, Object extraData){
        memorySessionRepository.updateExtraData(sessionId, extraData);
    }

    public static String continueSessionMessage(@NonNull String message){
        if(message.contains(ContextManager.getItem(Item.USSD_CONTINUE, String.class)))
            return message;
        return (ContextManager.getItem(Item.USSD_CONTINUE, String.class).concat(message));
    }

    public static String endSessionMessage(@NonNull String message){
        if(message.contains(ContextManager.getItem(Item.USSD_END, String.class)))
            return message;
        return (ContextManager.getItem(Item.USSD_END, String.class).concat(message));
    }
}
