package com.backend.api.ussdservice.ussd_reflection.bootstrap;

import com.backend.api.ussdservice.ussd_reflection.constants.CustomerTypes;
import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.Item;
import com.backend.api.ussdservice.ussd_reflection.context.exception.InvalidShortCodeException;
import com.backend.api.ussdservice.ussd_reflection.context.helper.GenericHelper;
import com.backend.api.ussdservice.ussd_reflection.context.model.UssdSession;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdPayload;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

import static com.backend.api.ussdservice.ussd_reflection.session.SessionManager.endSessionMessage;

@Component
@Slf4j
public class UssdGlobalServiceHandler
{
    private SessionManager sessionManager = new SessionManager();

    private static final String SPLIT_REGEX = "\\*";

    public String submitForContinuation(UssdPayload ussdPayload) throws Exception {

        String exceptionMessage = Strings.EMPTY;
        String fullUssdCode;
        UssdSession session = this.getUssdSession(ussdPayload);
        String contextData = session.getContextData();
        String shortCode = ussdPayload.getShortCodeString();

        // Provide support for short code string by mocking the session, context data and input.
        boolean isInputAbsent = ussdPayload.getInput() == null ||
                ussdPayload.getInput().isBlank() ||
                ussdPayload.getInput().isEmpty();
        if(isInputAbsent && shortCode != null && !shortCode.equalsIgnoreCase(this.getUssdBaseCode())){
            List<Integer> indices = GenericHelper.getIndicesOfStar(shortCode);
            int beginIndex = indices.get(1) + 1;
            int lastIndex = indices.get(indices.size() - 1);
            contextData = shortCode.substring(beginIndex, shortCode.lastIndexOf("#"));
            contextData = this.getActionableContextData(contextData, ussdPayload);
            session.setContextData(contextData);
            session.setShortCodeStarted(true);
            sessionManager.save(session);

            // At this point, the user is continuing ussd context. Prepare the full ussd code.
            String baseCodeWithoutHash = this.cleanBaseCode(this.getUssdBaseCode());
            contextData = getPreviousContextData(contextData);
            String relativeCode = this.getRelativeCodeFromContextData(contextData);
            String continuationInput = ussdPayload.getInput();
            if(relativeCode.isBlank() || relativeCode.isEmpty()){
                if(continuationInput == null){
                    fullUssdCode = baseCodeWithoutHash.concat("#");
                }else{
                    fullUssdCode = baseCodeWithoutHash.concat("*").concat(continuationInput).concat("#");
                }
            }else{
                if(continuationInput == null){
                    fullUssdCode = baseCodeWithoutHash.concat("*").concat(relativeCode).concat("#");
                }else{
                    fullUssdCode = baseCodeWithoutHash.concat("*").concat(relativeCode).concat("*").concat(continuationInput).concat("#");
                }
            }

            fullUssdCode = this.doNewUseRegFullCode(fullUssdCode, session);
            log.info("Full USSD Code: {} <==> Originating context data: {}", fullUssdCode, contextData);

            return executeActionForShortCodeStringAndContext(ussdPayload, session, fullUssdCode, contextData);
        }

        // At this point, the user is continuing ussd context. Prepare the full ussd code.
        String baseCodeWithoutHash = this.cleanBaseCode(this.getUssdBaseCode());
        contextData = getPreviousContextData(contextData);
        String relativeCode = this.getRelativeCodeFromContextData(contextData);
        String continuationInput = ussdPayload.getInput();
        if(relativeCode.isBlank() || relativeCode.isEmpty()){
            if(continuationInput == null){
                fullUssdCode = baseCodeWithoutHash.concat("#");
            }else{
                fullUssdCode = baseCodeWithoutHash.concat("*").concat(continuationInput).concat("#");
            }
        }else{
            if(continuationInput == null){
                fullUssdCode = baseCodeWithoutHash.concat("*").concat(relativeCode).concat("#");
            }else{
                fullUssdCode = baseCodeWithoutHash.concat("*").concat(relativeCode).concat("*").concat(continuationInput).concat("#");
            }
        }

        fullUssdCode = this.doNewUseRegFullCode(fullUssdCode, session);

        String isCorrectMainMenuSelected = checkIsCorrectMainMenuSelected(fullUssdCode, session);
        if(isCorrectMainMenuSelected != null){
            return isCorrectMainMenuSelected;
        }

        fullUssdCode = this.removePinStageFromUssdFullCode(fullUssdCode);
        log.info("Full USSD Code: {} <==> Originating context data: {}", fullUssdCode, contextData);

        return executeActionForFullUssdCodeAndContext(ussdPayload, session, fullUssdCode, contextData);
    }

    private String doNewUseRegFullCode(String fullUssdCode, UssdSession session){
        String splittable = fullUssdCode.substring(1);
        String[] split = splittable.split(SPLIT_REGEX);
        String fullNewUserCode = fullUssdCode;
        Object customerData = SessionManager.getExtraDataOfSession(session.getSessionId());
        String customerType = "";

        if(customerData != null) {
            if (customerData instanceof String) {
                customerType = (String) customerData;
            } else {
                HashMap<String, Object> customerDataHash = SessionManager.getExtraDataOfSession(session.getSessionId(), HashMap.class);
                customerType = String.valueOf(customerDataHash.get("customerType"));
            }
        }

        if(split.length >= 4 && customerData != null) {
            log.info("Customer Type found: {}", customerType);
            if (split[2].equalsIgnoreCase("1") && customerType.equalsIgnoreCase(CustomerTypes.NEW.name())) {
                List<String> tokens = new ArrayList<>();
                for(String s : split){
                    tokens.add(s.trim());
                }
                tokens.add(2, CustomerTypes.NEW.name());
                fullNewUserCode = "*".concat(String.join("*", tokens));
                log.info("Full New User code: {}", fullNewUserCode);
            }
        }
        return fullNewUserCode;
    }

    private String executeActionForFullUssdCodeAndContext(UssdPayload ussdPayload, UssdSession session, String fullUssdCode, String contextData) throws Exception{

        String exceptionMessage = Strings.EMPTY;

        // Build the UssdContext object
        UssdContext ussdContext = this.buildUssdContext(contextData, ussdPayload);

        // Get the method name that has this mapping of full code defined from the Ussd Context
        String invocableMethodName = ContextManager.getInvocableMethodNameByMapping(fullUssdCode);
        if(invocableMethodName == null){
            return executeActionForShortCodeStringAndContext(ussdPayload, session, fullUssdCode, contextData);
        }

        // Get the object that has this method name and the mapping as unique
        Object reflectObject = ContextManager.getActionableObjectByMappingAndMethodName(fullUssdCode, invocableMethodName);

        // Invoke the method and get Ussd response message.
        assert reflectObject != null;
        Method method = reflectObject.getClass().getDeclaredMethod(invocableMethodName, UssdContext.class);
        String message = (String) method.invoke(reflectObject, ussdContext);

        // Update the context data of the session if it is not a short string ussd invocation
        if(session != null) {
            updateUssdSession(contextData, session, ussdPayload);
        }

        return message;
    }

    private String executeActionForShortCodeStringAndContext(UssdPayload ussdPayload, UssdSession session, String fullUssdCode, String contextData) throws Exception{
        String exceptionMessage = Strings.EMPTY;

        // Build the UssdContext object
        UssdContext ussdContext = this.buildUssdContext(contextData, ussdPayload);
        String methodName = ContextManager.getInvocableMethodNameByMappingLength(fullUssdCode);
        System.out.println("Method Name: ==============> " + methodName);
        if(methodName != null) {
            String annotatedMapping = ContextManager.getMappingFromMethodName(methodName);
            Object reflectObject = ContextManager.getActionableObjectByMappingAndMethodName(annotatedMapping, methodName);
            assert reflectObject != null;
            List<String> paramHolders = GenericHelper.getPlaceHoldersNamesFromMapping(annotatedMapping);
            List<Integer> paramIndices = GenericHelper.getIndicesOfPlaceholdersFromMapping(annotatedMapping);
            List<String> paramValues = GenericHelper.getListOfParamValuesReplacingPlaceholders(fullUssdCode, paramIndices);

            List<Class<?>> classList = new ArrayList<>();
            for (int i = 0; i < paramIndices.size(); i++) {
                classList.add(String.class);
            }
            classList.add(0, UssdContext.class);
            Method method = reflectObject.getClass().getDeclaredMethod(methodName, classList.toArray(new Class[]{}));

            List<Object> methodParameters = new ArrayList<>();
            methodParameters.add(ussdContext);
            methodParameters.addAll(paramValues);

            // Update the context data of the session
            if (session != null) {
                updateUssdSession(contextData, session, ussdPayload);
            }
            return (String) method.invoke(reflectObject, methodParameters.toArray(new Object[]{}));
        }

        exceptionMessage = String.format("No Ussd mapping found for %s provided", fullUssdCode);
        throw new InvalidShortCodeException(exceptionMessage, ussdContext);

    }

    private void updateUssdSession(String workingContextData, UssdSession session, UssdPayload ussdPayload){
        String forwardOption = ContextManager.getItem(Item.DEFAULT_USSD_NEXT_OPTION, String.class);
        if(ussdPayload.getInput() != null &&
                !ussdPayload.getInput().isEmpty() &&
                !ussdPayload.getInput().isBlank() &&
                !ussdPayload.getInput().equalsIgnoreCase(forwardOption)
        )
        {
            String newContextData = workingContextData.concat("*").concat(ussdPayload.getInput());
            session.setContextData(newContextData);
            sessionManager.save(session);
        }
    }

    private UssdContext buildUssdContext(String contextData, UssdPayload ussdPayload){
        return UssdContext.builder()
                .originatingContextData(contextData)
                .input(ussdPayload.getInput())
                .mobileNumber(ussdPayload.getMobileNumber())
                .telco(ussdPayload.getTelco())
                .sessionOperation(ussdPayload.getSessionOperation())
                .sessionId(ussdPayload.getSessionId())
                .sessionType(ussdPayload.getSessionType())
                .build();
    }
    private String getActionableContextData(UssdSession ussdSession){
        String potentialContextData = ussdSession.getContextData();
        String contextDataPrefix = String.join("*", ussdSession.getMobileNumber(), ussdSession.getTelco());
        return (potentialContextData == null || potentialContextData.equalsIgnoreCase(Strings.EMPTY)) ? contextDataPrefix : potentialContextData;
    }

    private String getActionableContextData(String relativeCode, UssdPayload ussdPayload){
        return String.join("*", ussdPayload.getMobileNumber(), ussdPayload.getTelco(), relativeCode);
    }

    private UssdSession getUssdSession(UssdPayload ussdPayload){
        UssdSession ussdSession = sessionManager.findBySessionId(ussdPayload.getSessionId());
        if(ussdSession == null){
            ussdSession = new UssdSession();
            ussdSession.setSessionId(ussdPayload.getSessionId());
            ussdSession.setSessionStartDate(new Date());
            ussdSession.setTelco(ussdPayload.getTelco());
            ussdSession.setMobileNumber(ussdPayload.getMobileNumber());
            ussdSession.setContextData(this.getActionableContextData(ussdSession));
            return sessionManager.save(ussdSession);
        }
        return ussdSession;
    }

    private String cleanBaseCode(String baseCode){
        String result = baseCode;
        if(baseCode.endsWith("#")){
            result = baseCode.substring(0, baseCode.lastIndexOf("#"));
        }
        return result;
    }

    private String getRelativeCodeFromContextData(String contextData){
        String[] contextTokens = contextData.split(SPLIT_REGEX);
        List<String> codeTokens = new ArrayList<>(Arrays.asList(contextTokens).subList(2, contextTokens.length));
        return String.join("*", codeTokens);
    }

    private String getUssdBaseCode(){
        return ContextManager.getItem(Item.DEFAULT_USSD_BASE_CODE, String.class);
    }

    private String getPreviousContextData(String contextData){
        String goBackOption = ContextManager.getItem(Item.DEFAULT_USSD_GO_BACK_OPTION, String.class);
        String absoluteGoBackOptionEnd = "*".concat(goBackOption);
        if(contextData.endsWith(absoluteGoBackOptionEnd)){
          int stopIndex = GenericHelper.getSecondToLastIndexOfStar(contextData);
          int lastIndex = GenericHelper.getLastIndexOfStar(contextData);
          String returnable = contextData.substring(0, stopIndex);
            System.out.printf("Indices %s %s%n", stopIndex, lastIndex);
          if(contextData.split(SPLIT_REGEX).length >= 4)
              return returnable;
          return contextData.substring(0, lastIndex);
        }
        return contextData;
    }

    public String checkIsCorrectMainMenuSelected(String fullUssdCode, UssdSession session){
        // Check that the full code is among the top main menu
        String[] tokens = fullUssdCode.substring(1, fullUssdCode.lastIndexOf("#")).split("\\*");

        // Here the PIN is entered
        if(tokens.length == 4 && !List.of(tokens).contains(CustomerTypes.NEW.name())){
            String pin = tokens[3];
            // Check that the format of the PIN is correct
            if(pin.length() < 4){
                SessionManager.clearSession(session.getSessionId());
                return endSessionMessage(new MessageLineBuilder()
                        .addLine("Oops! pin must be at least a 4 digit entry!")
                        .addLine("Please try again with correct PIN entry")
                        .addLine("Thank you.")
                        .toString());
            }
        }

        if(tokens.length >= 5){
            int mainMenuSelected = Integer.parseInt(tokens[4]);
            log.info("Main menu selected is: {}", mainMenuSelected);
            if(mainMenuSelected < 1 || mainMenuSelected > 10){
                SessionManager.clearSession(session.getSessionId());
                return SessionManager.endSessionMessage(new MessageLineBuilder()
                        .addLine("Oops!, you entered a wrong menu. Please try again selecting a correct menu.")
                        .addLine("Thank you.")
                        .toString());
            }
//            // TODO: To be removed once implementation from other engineer is merged.
//            Object customerType = SessionManager.getExtraDataOfSession(session.getSessionId());
//            if(customerType instanceof String){
//                if(((String) customerType).equalsIgnoreCase(CustomerTypes.EXISTING.name())) {
//                    if (mainMenuSelected == 1 || mainMenuSelected == 2) {
//                        return SessionManager.endSessionMessage(new MessageLineBuilder()
//                                .addLine("This menu has been implemented.")
//                                .addLine("Thank you.")
//                                .toString());
//                    }
//                }
//            }else{
//                if (mainMenuSelected == 1 || mainMenuSelected == 2) {
//                    return SessionManager.endSessionMessage(new MessageLineBuilder()
//                            .addLine("This menu has been implemented.")
//                            .addLine("Thank you.")
//                            .toString());
//                }
//            }
        }
        return null;
    }

    public String removePinStageFromUssdFullCode(String fullUssdCode) {
        String[] tokens = fullUssdCode.substring(1, fullUssdCode.lastIndexOf("#")).split("\\*");
        if(tokens.length >= 5){
           List<String> tokenList = new ArrayList<>();
           Collections.addAll(tokenList, tokens);
           tokenList.remove(3);
            return "*".concat(String.join("*", tokenList)).concat("#");
        }
        return fullUssdCode;
    }

}
