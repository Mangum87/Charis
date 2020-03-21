package com.charis.data;

import com.charis.data.Enum.Condition;
import java.util.Date;


/**
 * Encapsulates properties of nonsellable items.
 */
public final class NonSellableItem extends Item
{
    private String source;


    /**
     * Builds an item.
     * @param id ID used for barcode
     * @param date Date item was received
     * @param desc Additional description
     * @param quant Amount in stock
     * @param c Condition of item
     * @param value Cost of item
     * @param cat Category of item
     * @param source Source of donation
     * @param loc Location of item
     */
    public NonSellableItem(String id, Date date, String desc, int quant, Condition c, double value, Category cat, String source, Location loc)
    {
        super(id, date, desc, quant, c, value, cat, loc);
        this.setSource(source);
    }

    public String getSource() {
        return source;
    }


    /**
     * Defaults to "" if source is null.
     * @param source Source of item
     */
    public void setSource(String source)
    {
        if(source != null)
            this.source = source;
        else
            this.source = "";
    }
}
