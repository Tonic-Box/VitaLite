package com.tonic.data;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration representing the different account types in the game.
 */
public enum AccountType {
    NORMAL(0),
    IRONMAN(1),
    ULTIMATE_IRONMAN(2),
    HARDCORE_IRONMAN(3),
    GROUP_IRONMAN(4),
    HARDCORE_GROUP_IRONMAN(5),
    UNRANKED_GROUP_IRONMAN(6);

    /**
     * A map for looking up AccountType by varbit value.
     */
    private static final Map<Integer, AccountType> LOOKUP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(
                    AccountType::getVarbitValue,
                    Function.identity()
            ));

    private final int varbitValue;

    AccountType(int varbitValue) {
        this.varbitValue = varbitValue;
    }

    /**
     * Gets the varbit value associated with this account type.
     *
     * @return the varbit value
     */
    public int getVarbitValue() {
        return varbitValue;
    }

    /**
     * Looks up an AccountType by its varbit value.
     *
     * @param varbitValue the varbit value to lookup
     * @return the matching AccountType, or null if not found
     */
    public static AccountType fromVarbitValue(int varbitValue) {
        return LOOKUP.get(varbitValue);
    }
}