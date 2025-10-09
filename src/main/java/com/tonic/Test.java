package com.tonic;

public class Test {
    static byte[] or = new byte[24];
    public static void test()
    {
        if(or != null)
        {
            System.out.println("Non null 1");
        }

        if(null != or)
        {
            System.out.println("Non null 1");
        }
    }
}
