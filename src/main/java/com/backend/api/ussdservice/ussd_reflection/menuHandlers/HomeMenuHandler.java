package com.backend.api.ussdservice.ussd_reflection.menuHandlers;

import com.backend.api.ussdservice.ussd_reflection.annotation.UssdMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdSubMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.*;
import com.google.gson.Gson;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeAmounts;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeEventId;
import com.backend.api.ussdservice.ussd_reflection.constants.CustomerTypes;
import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import com.backend.api.ussdservice.ussd_reflection.templates.HomeMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.utils.Utils;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.AccountNumbersResponseDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.ChargeCustomerRequestPayload;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.UserCreationRequestDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.UserEmailOrPhoneVerifyRequestDTO;
import com.backend.api.ussdservice.ussd_reflection.web.services.AccountService;
import com.backend.api.ussdservice.ussd_reflection.web.services.CustomerService;
import com.backend.api.ussdservice.ussd_reflection.web.services.FundsTransferService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;

import static com.backend.api.ussdservice.ussd_reflection.session.SessionManager.continueSessionMessage;
import static com.backend.api.ussdservice.ussd_reflection.session.SessionManager.endSessionMessage;

/**
 * This class is a USSD menu handler for both existing and new customers.
 */
@UssdMenuHandler
@Slf4j
public class HomeMenuHandler
{

    @Autowired
    private CustomerService customerService;

    @Autowired
    private FundsTransferService fundsTransferService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ChargeEventId chargeEventId;

    private final HomeMenuTemplate homeMenuTemplate = new HomeMenuTemplate();
    private static final Gson gson = new Gson();

    @UssdSubMenuHandler
    public String showHome(UssdContext context){
        String phoneNumber = context.getMobileNumber();
        WayaPayUserWebResponse webResponse = customerService.authenticateCustomer(phoneNumber);
        log.info("Auth Response: {}", gson.toJson(webResponse));
        AccountNumbersResponseDTO responseDTO = accountService.getAccountNumbersForCustomer(phoneNumber);
        HashMap<String, Object> customerData = new HashMap<>();
        if(webResponse != null && webResponse.isStatus()) {
            customerData.put("customerType", CustomerTypes.EXISTING.name());
            customerData.put("customerDetails", webResponse.getData());
            log.info("Web Data: {}", (UserProfileDetailsData)webResponse.getData());
            if(responseDTO != null && responseDTO.isStatus()){
                List<AccountNumbersResponse> accountNumbersResponses = responseDTO.getData();
                if(accountNumbersResponses != null && !accountNumbersResponses.isEmpty()){
                    customerData.put("defaultAccountNumber", accountNumbersResponses.get(0).getAccountNo());
                }
            }
            SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
            return SessionManager.continueSessionMessage(homeMenuTemplate.getExistingCustomerWelcomeScreen());
        }else{
            customerData.put("customerType", CustomerTypes.NEW.name());
            SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
            return SessionManager.continueSessionMessage(homeMenuTemplate.getNewCustomerWelcomeScreen());
        }
    }


    // If the existing or new customer enters 1.
    @UssdSubMenuHandler("*{input}#")
    public String showMenuPage(UssdContext context, String input) {
        if(input.equalsIgnoreCase("0")){
            SessionManager.clearSession(context.getSessionId());
            return endSessionMessage(homeMenuTemplate.getCustomerWelcomeMessageScreen());
        }
        else if(input.equalsIgnoreCase("1")) {
            HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
            String  customerType = String.valueOf(customerData.get("customerType"));
            if (customerType.equalsIgnoreCase(CustomerTypes.EXISTING.name())) {
                return continueSessionMessage(new MessageLineBuilder()
                        .addLine("Enter your PIN or enter CANCEL to quit.")
                        .toString());
            }

            SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
            return SessionManager.continueSessionMessage(homeMenuTemplate.getNewCustomerHomeMenuListScreen());
        }
        else if(input.equalsIgnoreCase("2")){
            SessionManager.clearSession(context.getSessionId());
            return endSessionMessage(new MessageLineBuilder()
                    .addLine("Thank you for banking with us")
                    .toString());
        }
        else{
                SessionManager.clearSession(context.getSessionId());
                return endSessionMessage(new MessageLineBuilder()
                        .addLine("Oops!, seems you entered a wrong input.")
                        .addLine("Thank you.")
                        .toString());
            }
    }

    @UssdSubMenuHandler("*{input}*{pin}#")
    public String showMainMenuPage(UssdContext context, String input, String pin){

        // Check that the format of the PIN is correct
        if(pin.length() < 4){
            SessionManager.clearSession(context.getSessionId());
            return endSessionMessage(new MessageLineBuilder()
                    .addLine("Oops! pin must be at least a 4 digit entry!")
                    .addLine("Please try again with correct PIN entry")
                    .addLine("Thank you.")
                    .toString());
        }

        if(pin.equalsIgnoreCase("CANCEL")){
            SessionManager.clearSession(context.getSessionId());
            return endSessionMessage(new MessageLineBuilder()
                    .addLine("Thank you for banking with us.")
                    .toString());
        }

        // Charge the customer for USSD service
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        String selectedAccount = String.valueOf(customerData.get("defaultAccountNumber"));
        UserProfileDetailsData data = (UserProfileDetailsData) customerData.get("customerDetails");
        ChargeCustomerRequestPayload requestPayload = new ChargeCustomerRequestPayload();
        requestPayload.setAmount(ChargeAmounts.USSD_SERVICE_CHARGE);
        requestPayload.setEventId(chargeEventId.getTRANSFER_AND_TRANSACTION_FEE_EVENT_ID());
        requestPayload.setSenderName(String.join(" ", data.getFirstName(), data.getMiddleName(), data.getLastName()));
        requestPayload.setPaymentReference(Utils.generatePaymentReference());
        requestPayload.setCustomerAccountNumber(String.valueOf(selectedAccount));
        requestPayload.setTranNarration("USSD Service Charge");
        log.info("Body to charge customer for Ussd service: {}", gson.toJson(requestPayload));
        ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
        if (responsePayload.isStatus()) {
            customerData.put("isCustomerCharged", true);
        }
        log.info("Customer Charge Response IFT: {}", gson.toJson(responsePayload));

        // Show the main menus
        return SessionManager.continueSessionMessage(homeMenuTemplate.getExistingCustomerHomeMenuListScreen());
    }

    @UssdSubMenuHandler("*NEW*{inputUserType}#")
    public String showDonePage(UssdContext context, String inputUserType) {
        HashMap<String, Object> customData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if(inputUserType.equalsIgnoreCase("1")) {
            String customerType = String.valueOf(customData.get("customerType"));
            if (customerType.equalsIgnoreCase(CustomerTypes.NEW.name())) {
                return continueSessionMessage(new MessageLineBuilder().addLine("Enter first name or press 0 to go back.").toString());
            }
        }
        if(inputUserType.equalsIgnoreCase("2")){
            String customerType = String.valueOf(customData.get("customerType"));
            if(customerType.equalsIgnoreCase(CustomerTypes.NEW.name())){
                SessionManager.clearSession(context.getSessionId());
                return endSessionMessage(homeMenuTemplate.getNewCustomerBusinessLinkScreen());
            }
        }
        return Strings.EMPTY;
    }

    // Continue registration of new Personal Users
    @UssdSubMenuHandler("*NEW*{inputUserType}*{firstName}#")
    public String collectLastNameOfNewUser(UssdContext context, String inputUserType, String firstName){
        if(ContextManager.isGoBackOption(firstName)){
            return SessionManager.continueSessionMessage(homeMenuTemplate.getNewCustomerHomeMenuListScreen());
        }
        HashMap<String, String> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        customerData.put("firstName", firstName);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
        return continueSessionMessage(new MessageLineBuilder().addLine("Enter Last name or press 0 to go back.").toString());
    }

    @UssdSubMenuHandler("*NEW*{inputUserType}*{firstName}*{lastName}#")
    public String collectNewUserEmailAddress(UssdContext context, String in, String fN, String lastName){
        if(ContextManager.isGoBackOption(lastName)){
            return continueSessionMessage(new MessageLineBuilder().addLine("Enter first name or press 0 to go back.").toString());
        }
        HashMap<String, String> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        customerData.put("lastName", lastName);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
        return continueSessionMessage(new MessageLineBuilder().addLine("Enter email address or press 0 to go back.").toString());
    }

    @UssdSubMenuHandler("*NEW*{inputUserType}*{firstName}*{lastName}*{email}#")
    public String collectNewUserPhoneNumber(UssdContext context, String in, String fN, String lN, String email){
        if(ContextManager.isGoBackOption(email)){
            return continueSessionMessage(new MessageLineBuilder().addLine("Enter Last name or press 0 to go back.").toString());
        }
        HashMap<String, String> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        customerData.put("emailAddress", email);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
        return continueSessionMessage(new MessageLineBuilder().addLine("Enter phone number or press 0 to go back.").toString());
    }

    @UssdSubMenuHandler("*NEW*{inputUserType}*{firstName}*{lastName}*{email}*{phoneNumber}#")
    public String collectUserPassword(UssdContext context, String in, String fN, String lN, String e, String phoneNumber){
        if(ContextManager.isGoBackOption(phoneNumber)){
            return continueSessionMessage(new MessageLineBuilder().addLine("Enter email address or press 0 to go back.").toString());
        }
        HashMap<String, String> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        String formattedPhone = "";
        if(phoneNumber.startsWith("234")){
            formattedPhone = phoneNumber;
        }else{
            formattedPhone = "234".concat(phoneNumber.substring(1));
        }
        customerData.put("phone", formattedPhone);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
        return continueSessionMessage(new MessageLineBuilder()
                .addLine("Enter preferred password or press 0 to go back.")
                .addLine("Note: Password should be at least 8 characters long.")
                .toString());
    }

    @UssdSubMenuHandler("*NEW*{inputUserType}*{firstName}*{lastName}*{email}*{phoneNumber}*{password}#")
    public String doNewUserCreation(UssdContext context, String in, String fN, String lN, String e, String phone, String pwd){
        if(ContextManager.isGoBackOption(pwd)){
            return continueSessionMessage(new MessageLineBuilder().addLine("Enter phone number or press 0 to go back.").toString());
        }
        HashMap<String, String> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        UserCreationRequestDTO requestDTO = new UserCreationRequestDTO();
        requestDTO.setPassword(pwd);
        requestDTO.setAdmin(false);
        requestDTO.setEmail(String.valueOf(customerData.get("emailAddress")));
        requestDTO.setSurname(String.valueOf(customerData.get("lastName")));
        requestDTO.setUssd(true);
        requestDTO.setFirstName(String.valueOf(customerData.get("firstName")));
        requestDTO.setReferenceCode("");
        requestDTO.setPhoneNumber(String.valueOf(customerData.get("phone")));

        UserCreationResponseDTO responseDTO = customerService.processNewPersonalUserCreation(requestDTO);
        // TODO: Send otp
        if(responseDTO != null && responseDTO.isStatus()){
            return continueSessionMessage(new MessageLineBuilder()
                    .addLine("OTP has been sent to your phone number Also a message has been sent to email. Please Enter OTP sent to you.")
                    .addLine("If you did not receive OTP, enter email address used to register.")
                    .toString());
        }else{
            assert responseDTO != null;
            String message = responseDTO.getMessage() == null ? new MessageLineBuilder()
                    .addLine("Oops! internal system error occurred")
                    .addLine(String.format("Reason: %s", "Server failed to respond to account creation request."))
                    .addLine("Please try again or contact administrator or support.")
                    .addLine("Thank you.")
                    .toString() : this.getAppropriateRegistrationMessage(responseDTO.getData(), responseDTO.getMessage());
            SessionManager.clearSession(context.getSessionId());
            return endSessionMessage(message);
        }
    }

    @UssdSubMenuHandler("*NEW*{inputUserType}*{firstName}*{lastName}*{email}*{phoneNumber}*{password}*{otpOrEmail}#")
    public String validateOTPSentToNewUser(UssdContext context, String in, String fN, String lN, String e, String phone, String pwd, String otpOrEmail){
        UserEmailOrPhoneVerifyRequestDTO requestDTO = new UserEmailOrPhoneVerifyRequestDTO();
        HashMap<String, String> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        if(otpOrEmail.contains("@")){
            requestDTO.setPhoneOrEmail(otpOrEmail);
            requestDTO.setOtp("1234");
        }else {
            requestDTO.setOtp(otpOrEmail);
            requestDTO.setPhoneOrEmail(String.valueOf(customerData.get("phone")));
        }
        GenericResponseDTO responseDTO = customerService.verifyNewCustomerEmailOrPhone(requestDTO);
        if(responseDTO != null && responseDTO.isStatus()){
            return continueSessionMessage(new MessageLineBuilder().addLine("Enter your preferred PIN").toString());
        }else{
            assert responseDTO != null;
            String message = responseDTO.getMessage() == null ?
                    "Oops! otp could not be validated. Please try again." :
                    responseDTO.getMessage();
            SessionManager.clearSession(context.getSessionId());
            return endSessionMessage(new MessageLineBuilder().addLine(message).toString());
        }
    }

    @UssdSubMenuHandler("*NEW*{inputUserType}*{firstName}*{lastName}*{email}*{phoneNumber}*{password}*{otpOrEmail}*{pin}#")
    public String showAccountCreationSuccessMessage(UssdContext context, String in, String fN, String lN, String e, String phone, String pwd, String otpOrEmail, String pin){
        HashMap<String, String> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        customerData.put("pin", pin);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
        SessionManager.clearSession(context.getSessionId());
        return endSessionMessage(new MessageLineBuilder()
                .addLine("Account registration successful.")
                .addLine("Thank you for choosing WAYA MICROFINANCE BANK")
                .toString());
    }

    private String getAppropriateRegistrationMessage(Object data, String message) {
        String result = message;
        if(data instanceof List){
            List<String> errorList = (List<String>) data;
            result = errorList.get(0);
        }
        return result;
    }

}
