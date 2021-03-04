package com.charis.data;


import com.charis.data.Enum.Condition;
import java.util.Date;

/**
 * Encapsulates properties for items in inventory.
 */
public abstract class Item implements Comparable
{
    private final String ID;
    private Date received;
    private String description;
    private Condition condition;
    private int quantity;
    private double price;
    private Category category;
    private Location location;


    /**
     * Builds an item.
     * @param id ID used for barcode
     * @param date Date item was received
     * @param desc Additional description
     * @param quant Amount in stock
     * @param c Condition of item
     * @param price Cost of item
     * @param cat Category of item
     * @param loc Location of item
     */
    public Item(String id, Date date, String desc, int quant, Condition c, double price, Category cat, Location loc)
    {
        this.ID = id;
        this.setReceived(date);
        this.setDescription(desc);
        this.setCondition(c);
        this.setQuantity(quant);
        this.setPrice(price);
        this.setCategory(cat);
        this.setLocation(loc);
    }


    public String getID() {
        return ID;
    }

    public Date getReceived() {
        return received;
    }

    public void setReceived(Date received) {
        this.received = received;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public int getQuantity() {
        return quantity;
    }


    /**
     * Can be negative if reported by DB.
     * @param quantity Quantity in database
     */
    public void setQuantity(int quantity)
    {
        this.quantity = quantity;
    }


    /**
     * Decreases the quantity stored by the quantity passed.
     * Quantity passed must be greater than zero.
     * @param quantity Amount to decrease by
     */
    public void decreaseQuantity(int quantity)
    {
        if(quantity < 1)
            return;

        this.quantity -= quantity;
    }



    public double getPrice() {
        return price;
    }

    /**
     * Price must be positive, otherwise, defaults to 0.0
     * @param price Price of item
     */
    public void setPrice(double price)
    {
        if(price >= 0)
            this.price = price;
        else
            this.price = 0.0;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location)
    {
        if(location != null)
            this.location = location;
    }

    @Override
    public int compareTo(Object o)
    {
        SellableItem item = (SellableItem)o;
        return received.compareTo(item.getReceived());
    }
}
