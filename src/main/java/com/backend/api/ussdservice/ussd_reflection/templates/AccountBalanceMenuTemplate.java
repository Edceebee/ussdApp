package com.backend.api.ussdservice.ussd_reflection.templates;

import com.backend.api.ussdservice.pojo.UssdMessageAndData;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MenuOptionBuilder;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;

import java.util.HashMap;
import java.util.List;

/**
 * This class represents the templates for account balance menu.
 */
public class AccountBalanceMenuTemplate
{
    public String getAccountBalanceScreen(){
        return new MessageLineBuilder().addLine("Please enter your pin or press 0 to go back to previous menu")
                .toString();
    }

    public UssdMessageAndData getAccountNumberListUssdMessageAndDataFromCollection(List<String> collection, HashMap<String, Object> customerData, String key){
        MenuOptionBuilder menuOptionBuilder = new MenuOptionBuilder()
                .addOption("", "Select account to check balance");

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

    public String getEnterPinOrGoBackScreen(){
        return new MessageLineBuilder().addLine("Please enter your pin or press 0 to go back to previous menu")
                .toString();
    }

    public String getServiceChargeScreen(){
        return new MessageLineBuilder()
                .addLine("Transaction fee N10 applies.")
                .addLine("Do you wish to continue?")
                .addLine("1. Yes")
                .addLine("0. Back")
                .toString();
    }

    public String getAccountBalanceCheckMessage(String accountNumber, String accountBalance){
        return new MessageLineBuilder()
                .addLine("Your account balance from " + accountNumber + " is " + accountBalance)
                .addLine("Make transfer with ease on *347*006#. Simply dial *347*006*3# to do transfer to other banks today.")
                .toString();
    }

    public String getErrorMessage(UssdContext context){
        SessionManager.clearSession(context.getSessionId());
        return new MessageLineBuilder()
                .addLine("Oops! you entered an invalid input. Please try again.")
                .addLine("Thank you")
                .toString();
    }
}
