package com.charis.data;

import com.charis.data.Enum.Condition;
import java.util.Date;

public final class SellableItem extends Item
{
    /**
     * Builds a sellable item.
     * @param id ID used for barcode
     * @param date Date item was received
     * @param desc Additional description
     * @param quant Amount in stock
     * @param c Condition of item
     * @param price Price of item
     * @param cat Category of item
     * @param loc Location of item
     */
    public SellableItem(String id, Date date, String desc, int quant, Condition c, double price, Category cat, Location loc)
    {
        super(id, date, desc, quant, c, price, cat, loc);
    }
}
