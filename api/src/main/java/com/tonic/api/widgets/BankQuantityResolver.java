package com.tonic.api.widgets;

final class BankQuantityResolver
{
    private BankQuantityResolver()
    {
    }

    static boolean isPresetAmount(int amount)
    {
        return amount == 1 || amount == 5 || amount == 10 || amount == -1;
    }

    static String withdrawMenuAction(int amount)
    {
        switch (amount)
        {
            case 1:
                return "Withdraw-1";
            case 5:
                return "Withdraw-5";
            case 10:
                return "Withdraw-10";
            case -1:
                return "Withdraw-All";
            default:
                return "Withdraw-X";
        }
    }

    static int withdrawFallbackAction(int amount)
    {
        switch (amount)
        {
            case 1:
                return 1;
            case 5:
                return 3;
            case 10:
                return 4;
            case -1:
                return 7;
            default:
                return 5;
        }
    }

    static String depositMenuAction(int amount)
    {
        switch (amount)
        {
            case 1:
                return "Deposit-1";
            case 5:
                return "Deposit-5";
            case 10:
                return "Deposit-10";
            case -1:
                return "Deposit-All";
            default:
                return "Deposit-X";
        }
    }

    static int depositFallbackAction(int amount)
    {
        switch (amount)
        {
            case 1:
                return 2;
            case 5:
                return 4;
            case 10:
                return 5;
            case -1:
                return 8;
            default:
                return 6;
        }
    }
}
