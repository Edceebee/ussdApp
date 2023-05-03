package com.backend.api.ussdservice.ussd_reflection.templates;

import com.backend.api.ussdservice.pojo.UssdMessageAndData;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MenuOptionBuilder;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import org.apache.logging.log4j.util.Strings;

import java.util.HashMap;
import java.util.List;

public class DataMenuTemplate
{
    public UssdMessageAndData getAccountNumberListUssdMessageAndDataFromCollection(List<String> collection, HashMap<String, Object> customerData, String key){
        MenuOptionBuilder menuOptionBuilder = new MenuOptionBuilder()
                .addOption("", "Select account");

        for(int i = 1; i <= collection.size(); i++){
            String accountNumber = collection.get(i - 1);
            String accountKey = key.concat(String.valueOf(i));
            customerData.put(accountKey, accountNumber);
            menuOptionBuilder.addOption(i, accountNumber);
        }

        menuOptionBuilder.addOption(0, "Go back");
        customerData.put("accountNumbers", collection);

        return UssdMessageAndData.builder()
                .message(menuOptionBuilder.toString())
                .customerData(customerData)
                .build();
    }

    public UssdMessageAndData getDataPlanListUssdMessageAndDataFromCollection(List<String> collection, HashMap<String, Object> customerData, String key){
        MenuOptionBuilder menuOptionBuilder = new MenuOptionBuilder()
                .addOption(Strings.EMPTY, "Select data plan ");

        for(int i = 1; i <= collection.size(); i++){
            String dataPlan = collection.get(i - 1);
            String dataPlanKey = key.concat(String.valueOf(i));
            customerData.put(dataPlanKey, dataPlan);
            menuOptionBuilder.addOption(i, dataPlan);
        }

        menuOptionBuilder.addOption(0, "Go back");
        customerData.put("dataPlans", collection);

        return UssdMessageAndData.builder()
                .message(menuOptionBuilder.toString())
                .customerData(customerData)
                .build();
    }

    public String getEnterBeneficiaryPhoneNumber(){
        return new MessageLineBuilder()
                .addLine("Enter beneficiary phone number: ")
                .toString();
    }

    public String getEnterPINMessageScreen(){
        return new MessageLineBuilder()
                .addLine("Enter (4) digit pin ")
                .toString();
    }

    public String getErrorMessage(UssdContext context){
        String message = new MessageLineBuilder()
                .addLine("Oops!. You entered an invalid input. Please try again")
                .addLine("Thank you")
                .toString();
        SessionManager.clearSession(context.getSessionId());
        return message;
    }
}
