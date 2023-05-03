package com.backend.api.ussdservice.ussd_reflection.bootstrap;

import com.backend.api.ussdservice.ussd_reflection.annotation.UssdMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdMessageType;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdSubMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.Item;
import com.backend.api.ussdservice.ussd_reflection.context.exception.IllegalUssdHandlerMethodDefinitionException;
import com.backend.api.ussdservice.ussd_reflection.context.exception.UniqueUssdMappingViolationException;
import com.backend.api.ussdservice.ussd_reflection.context.helper.GenericHelper;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.AppUserHandshakeResponse;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.BankData;
import com.backend.api.ussdservice.ussd_reflection.web.services.AppUserService;
import com.backend.api.ussdservice.ussd_reflection.web.services.FundsTransferService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Stream;

@Configuration
@Slf4j
@Service
public class UssdContextBootstrap
{
    @Autowired
    private ApplicationContext context;

    private static final Reflections applicationPackageReflection = new Reflections("com.wayapaychat.backend.api.ussdservice.ussd_reflection");

    @Value("${ussd.baseCode:null}")
    private String baseCode;

    @Value("${ussd.menu.goBackOption:null}")
    private String ussdGoBackOption;

    @Value("${ussd.menu.nextOption:null}")
    private String ussdNextOption;

    @Value("${ussd.logging.verbose:true}")
    private boolean showLogging;

    @Value("${ussd.session.repository:memory}")
    private String ussdSessionRepositoryType;

    @Value("${session.continue}")
    private String SESSION_CONTINUE;

    @Value("${session.end}")
    private String SESSION_END;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private FundsTransferService fundsTransferService;


    @Bean
    public String startUp(){
        String potentialBaseCode = Strings.EMPTY;
        if(baseCode == null || baseCode.equalsIgnoreCase("null")){
            log.info("Could not find USSD base code in configuration properties file. Will use {} as default", baseCode);
            potentialBaseCode = baseCode;
        }else {
            log.info("Found USSD base code of {} in configuration properties file.", baseCode);
            potentialBaseCode = baseCode;
        }

        String goBackOption = Strings.EMPTY;
        if(ussdGoBackOption == null || ussdGoBackOption.equalsIgnoreCase("null")){
            log.info("Could not find defined Ussd goBackOption for menus in configuration properties file. Will use {} as default", ussdGoBackOption);
            goBackOption = ussdGoBackOption;
        }else{
            log.info("Found defined Ussd goBackOption of {} in configuration properties file.", ussdGoBackOption);
            goBackOption = ussdGoBackOption;
        }

        String nextOption = Strings.EMPTY;
        if(ussdNextOption == null || ussdNextOption.equalsIgnoreCase("null")){
            log.info("Could not find defined Ussd nextOption for menus in configuration properties file. Will use {} as default", ussdNextOption);
            nextOption = ussdNextOption;
        }else{
            log.info("Found defined Ussd nextOption of {} in configuration properties file.", ussdNextOption);
            nextOption = ussdNextOption;
        }

        if(ussdSessionRepositoryType.equalsIgnoreCase("memory")){
            log.info("In-memory session storage initialized for Ussd session management.");
        }
        else if (ussdSessionRepositoryType.equalsIgnoreCase("database")) {
            log.info("Using defined database schema for Ussd session management.");
        }
        else{
            log.info("Could not find configuration for Ussd session storage. Will use in-memory as default");
        }

        ContextManager.setItem(Item.DEFAULT_USSD_BASE_CODE, potentialBaseCode);
        ContextManager.setItem(Item.DEFAULT_USSD_GO_BACK_OPTION, goBackOption);
        ContextManager.setItem(Item.DEFAULT_USSD_NEXT_OPTION, nextOption);
        ContextManager.setItem(Item.USSD_SESSION_STORAGE, ussdSessionRepositoryType);
        ContextManager.setItem(Item.USSD_CONTINUE, SESSION_CONTINUE);
        ContextManager.setItem(Item.USSD_END, SESSION_END);
        return Strings.EMPTY;
    }

     @Bean
     public String bootstrap(){
        handleUssdMessageTypesBootstrap();
        handleMappingAndMethodBootstrap();
        return Strings.EMPTY;
     }

     @Bean
     public String credentialBootstrap(){
         AppUserHandshakeResponse response = appUserService.getAppUserCredentials();
         if(response != null && response.isStatus()){
             if(response.getData().getToken() != null &&
                     !response.getData().getToken().isEmpty() &&
             !response.getData().getToken().isBlank()){
                 log.info("App User credentials saved to USSD context keeper");
             }
             ContextManager.setItem(Item.USSD_APP_USER_TOKEN, response.getData().getToken());
         }
         return Strings.EMPTY;
     }

     @Bean String banksBootstrap(){
         List<BankData> data = fundsTransferService.getBanks();
         if(data != null){
             log.info("Banks and codes saved to the USSD context");
         }
         ContextManager.setItem(Item.USSD_SERVICE_BANKS, data);
         return Strings.EMPTY;
     }

     private void handleMappingAndMethodBootstrap(){

         Set<Class<?>> ussdHandlers = applicationPackageReflection.getTypesAnnotatedWith(UssdMenuHandler.class);
         log.info("Auto scanning completed, found {} classes annotated with the @UssdMenuHandler", ussdHandlers.size());

         Map<List<String>, Object> classToMethods = new HashMap<>();
         List<String> normalizedMappingTrackList = new ArrayList<>();
         for(Class<?> ussdHandler : ussdHandlers) {
             Object reflectObject = context.getBean(ussdHandler);
             UssdMenuHandler annotation = ussdHandler.getAnnotation(UssdMenuHandler.class);
             String annotationValue = annotation.value().trim();

             if(!annotationValue.isEmpty() && !annotationValue.startsWith("*")){
                 annotationValue = "*".concat(annotationValue);
             }

             String baseUssdCode = ContextManager.getItem(Item.DEFAULT_USSD_BASE_CODE, String.class);

             if(baseUssdCode.endsWith("#"))
                 baseUssdCode = baseUssdCode.substring(0, baseUssdCode.indexOf("#"));

             final String finalBaseCode = baseUssdCode.concat(annotationValue);
             Method[] declaredMethods = ussdHandler.getDeclaredMethods();
             List<String> methodAndMapping = new ArrayList<>();
             Stream.of(declaredMethods).forEach(method -> {
                 if(method.isAnnotationPresent(UssdSubMenuHandler.class)){
                     String methodName = method.getName();
                     method.setAccessible(true);
                     UssdSubMenuHandler optionHandler = method.getAnnotation(UssdSubMenuHandler.class);
                     String optionValue = GenericHelper.cleanMapping(optionHandler.value().trim());
                     optionValue = GenericHelper.cleanMapping(optionValue);
                     if(!optionValue.isEmpty() && !optionValue.equalsIgnoreCase("#") && !optionValue.startsWith("*"))
                         optionValue = "*".concat(optionValue);
                     if(optionValue.isEmpty() || optionValue.equalsIgnoreCase("#")){
                         optionValue = "#";
                     }
                     String completeUssdMapping = finalBaseCode.concat(optionValue);
                     if(ContextManager.isMappingAlreadyExists(completeUssdMapping)){
                         String currentClassName = ussdHandler.getName();
                         String message = String.format("\nMessage: Error creating mapping %s in class %s. \nReason: mapping already defined in same or another class. \nPossible solution: Consider defining a different mapping", completeUssdMapping, currentClassName);
                         throw new UniqueUssdMappingViolationException(message);
                     }
                     if(GenericHelper.isMappingContainsPlaceHolders(completeUssdMapping)){
                         String normalizedPlaceHolderMapping = GenericHelper.getNormalizeMappingString(completeUssdMapping);
                         String currentClassName = ussdHandler.getName();
                         String message = String.format("\nMessage: Error creating mapping of the form %s in class %s. \nReason: mapping already defined in same or another class. \nPossible solution: Consider defining a different mapping", normalizedPlaceHolderMapping, currentClassName);

                         // Check for mapping violation or else add to the mapping list.
                         if(normalizedMappingTrackList.contains(normalizedPlaceHolderMapping))
                             throw new UniqueUssdMappingViolationException(message);
                         normalizedMappingTrackList.add(normalizedPlaceHolderMapping);
                     }

                     // Validate the parameter listing for this method definition.
                     Map<Boolean, List<String>> oValidateResult = this.validateParameterCompleteAndWellDefinedForMapping(completeUssdMapping, method, ussdHandler.getName());
                     boolean bool = new ArrayList<>(oValidateResult.entrySet()).get(0).getKey();
                     List<String> possibleErrors = new ArrayList<>(oValidateResult.entrySet()).get(0).getValue();
                     if(!bool)
                         throw new IllegalUssdHandlerMethodDefinitionException(possibleErrors);

                     methodAndMapping.add(completeUssdMapping.concat(methodName));
                     ContextManager.saveMapping(completeUssdMapping, methodName);
                 }
             });

             classToMethods.put(methodAndMapping, reflectObject);
         }

         // Log the mappings and their invocable methods.
         if(showLogging) {
             logMappingsAndInvocableMethods();
         }
         ContextManager.parentObjectMappings = classToMethods;
     }

     private void handleUssdMessageTypesBootstrap(){
         Set<Class<?>> ussdHandlers = applicationPackageReflection.getTypesAnnotatedWith(UssdMessageType.class);
         ContextManager.setItem(Item.USSD_MESSAGE_RETURN_TYPE, ussdHandlers);
     }

     private Map<Boolean, List<String>> validateParameterCompleteAndWellDefinedForMapping(String mapping, Method method, String clazzName){
        Map<Boolean, List<String>> result = new HashMap<Boolean, List<String>>();
        boolean bool = Boolean.TRUE;

        List<String> errorMessages = new ArrayList<>();
        int countOfPlaceHolders = GenericHelper.getCountOfPlaceHoldersInMapping(mapping);
        List<Parameter> parameters = List.of(method.getParameters());
        String message;
        if(parameters.isEmpty() || parameters.get(0).getType() != UssdContext.class){
            message = String.format("First parameter of method %s defined in class %s must be an instance of UssdContext. This is mandatory for all UssdHandlers.", method.getName(), clazzName);
            bool = Boolean.FALSE;
            errorMessages.add(message);
        }
        if(parameters.size() != (countOfPlaceHolders + 1)){
            message = String.format("Invalid parameter definition of method %s defined in class %s. The number of parameters after UssdContext parameter must exactly match the number of place holders in @UssdSubMenuHandler annotation defined in method.", method.getName(), clazzName);
            bool = Boolean.FALSE;
            errorMessages.add(message);
        }
        for(Parameter parameter : parameters){
            if(parameter.getType() != UssdContext.class){
                if(parameter.getType() != String.class){
                    message = String.format("Invalid type for parameter %s in method %s defined in class %s. All types for the parameter mapping to a Ussd param defined in annotation must be a String.", parameter.getName(), method.getName(), clazzName);
                    bool = Boolean.FALSE;
                    errorMessages.add(message);
                }
            }
        }
        if(bool == Boolean.FALSE){
            result.put(false, errorMessages);
        }else {
            result.put(true, new ArrayList<>());
        }

        return result;
     }

     private void logMappingsAndInvocableMethods(){
        GenericHelper.printUssdMappingLogHeader();
        List<Map.Entry<String, String>> mappingList = new ArrayList<>(ContextManager.optionMappings.entrySet());
        Collections.reverse(mappingList);
        for(Map.Entry<String, String> entry : mappingList){
            String mapping = entry.getKey();
            String invocableMethodName = entry.getValue();
            String loggable = "{ ussdMapping = ".concat(mapping).concat(", invocableMethod = ")
                              .concat(invocableMethodName).concat(" }");
            System.out.println(GenericHelper.getNoOfTimes(" ", 4).concat(loggable));
        }
        GenericHelper.printUssdMappingFooter();
     }

}
