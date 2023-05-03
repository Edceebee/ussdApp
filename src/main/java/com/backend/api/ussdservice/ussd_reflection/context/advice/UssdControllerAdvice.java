package com.backend.api.ussdservice.ussd_reflection.context.advice;

import com.backend.api.ussdservice.ussd_reflection.context.exception.InvalidShortCodeException;
import com.backend.api.ussdservice.ussd_reflection.context.exception.NoUssdMappingFoundException;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import com.backend.api.ussdservice.controller.UssdMenuController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@RestControllerAdvice(basePackageClasses = { UssdMenuController.class })
public class UssdControllerAdvice
{

    @ExceptionHandler(NoUssdMappingFoundException.class)
    @ResponseStatus(HttpStatus.OK)
    public void handleNoUssdMappingException(HttpServletResponse response, NoUssdMappingFoundException e) throws IOException {
        PrintWriter writer = response.getWriter();
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        SessionManager.clearSession(e.getUssdContext().getSessionId());
        writer.write(e.getMessage());
    }

    @ExceptionHandler(InvalidShortCodeException.class)
    @ResponseStatus(HttpStatus.OK)
    public void handleInvalidShortCodeException(HttpServletResponse response, NoUssdMappingFoundException e) throws IOException {
        PrintWriter writer = response.getWriter();
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        SessionManager.clearSession(e.getUssdContext().getSessionId());
        writer.write(e.getMessage());
    }

}
