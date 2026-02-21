package com.tonic.api.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankQuantityResolverTest
{
    @Test
    void presetDetectionIsStable()
    {
        assertTrue(BankQuantityResolver.isPresetAmount(1));
        assertTrue(BankQuantityResolver.isPresetAmount(5));
        assertTrue(BankQuantityResolver.isPresetAmount(10));
        assertTrue(BankQuantityResolver.isPresetAmount(-1));
        assertFalse(BankQuantityResolver.isPresetAmount(2));
        assertFalse(BankQuantityResolver.isPresetAmount(50));
    }

    @Test
    void withdrawMappingMatchesBankMenuContract()
    {
        assertEquals("Withdraw-1", BankQuantityResolver.withdrawMenuAction(1));
        assertEquals("Withdraw-5", BankQuantityResolver.withdrawMenuAction(5));
        assertEquals("Withdraw-10", BankQuantityResolver.withdrawMenuAction(10));
        assertEquals("Withdraw-All", BankQuantityResolver.withdrawMenuAction(-1));
        assertEquals("Withdraw-X", BankQuantityResolver.withdrawMenuAction(42));

        assertEquals(1, BankQuantityResolver.withdrawFallbackAction(1));
        assertEquals(3, BankQuantityResolver.withdrawFallbackAction(5));
        assertEquals(4, BankQuantityResolver.withdrawFallbackAction(10));
        assertEquals(7, BankQuantityResolver.withdrawFallbackAction(-1));
        assertEquals(5, BankQuantityResolver.withdrawFallbackAction(42));
    }

    @Test
    void mixedWithdrawAmountsRemainDeterministic()
    {
        int[] amounts = {3, -1, 1, -1, 7};
        String[] expectedMenuActions = {"Withdraw-X", "Withdraw-All", "Withdraw-1", "Withdraw-All", "Withdraw-X"};
        int[] expectedFallbacks = {5, 7, 1, 7, 5};

        for (int i = 0; i < amounts.length; i++)
        {
            assertEquals(expectedMenuActions[i], BankQuantityResolver.withdrawMenuAction(amounts[i]));
            assertEquals(expectedFallbacks[i], BankQuantityResolver.withdrawFallbackAction(amounts[i]));
        }
    }

    @Test
    void depositMappingMatchesBankMenuContract()
    {
        assertEquals("Deposit-1", BankQuantityResolver.depositMenuAction(1));
        assertEquals("Deposit-5", BankQuantityResolver.depositMenuAction(5));
        assertEquals("Deposit-10", BankQuantityResolver.depositMenuAction(10));
        assertEquals("Deposit-All", BankQuantityResolver.depositMenuAction(-1));
        assertEquals("Deposit-X", BankQuantityResolver.depositMenuAction(42));

        assertEquals(2, BankQuantityResolver.depositFallbackAction(1));
        assertEquals(4, BankQuantityResolver.depositFallbackAction(5));
        assertEquals(5, BankQuantityResolver.depositFallbackAction(10));
        assertEquals(8, BankQuantityResolver.depositFallbackAction(-1));
        assertEquals(6, BankQuantityResolver.depositFallbackAction(42));
    }
}
