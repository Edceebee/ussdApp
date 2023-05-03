package com.backend.api.ussdservice.ussd_reflection.context.ussd.message;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MenuOptionBuilder
{
    private final List<String> menuOptions = new ArrayList<>();
    public MenuOptionBuilder addOption(String optionNo, String option){
        String combinedOptionMessage = optionNo.trim().concat(". ").concat(option.trim());
        menuOptions.add(combinedOptionMessage);
        return this;
    }

    public MenuOptionBuilder addOption(int optionNo, String option){
        String optionNoString = String.valueOf(optionNo);
        addOption(optionNoString, option);
        return this;
    }

    public String toString() {
        if(menuOptions.isEmpty())
            return "";
        return String.join("\n", menuOptions);
    }
}
