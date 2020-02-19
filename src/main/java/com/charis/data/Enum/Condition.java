package com.charis.data.Enum;

public enum Condition
{
    POOR, GOOD, EXCELLENT;


    /**
     * Returns a Condition object from corresponding int.
     * @param c int to translate
     * @return Condition object
     */
    public static Condition toCondition(int c)
    {
        switch(c)
        {
            case 0:
                return POOR;
            case 1:
                return GOOD;
            default:
                return EXCELLENT;
        }
    }


    /**
     * Returns a Condition object from corresponding long.
     * @param c long to translate
     * @return Condition object
     */
    public static Condition toCondition(long c)
    {
        String s = String.valueOf(c); // Convert to string
        return toCondition(Integer.valueOf(s)); // Convert to int
    }


    /**
     * Returns the int equivalent of given Condition.
     * @param c Condition object
     * @return int equivalent of c
     */
    public static int toInt(Condition c)
    {
        if(c == POOR)
            return 0;
        else if(c == GOOD)
            return 1;
        else
            return 2;
    }
}
