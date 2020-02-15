package com.charis.data;


import java.util.Date;

/**
 * Encapsulates data for distributing items
 * for sale or to give away.
 */
public final class Distribution
{
    private final String ID;
    private double amount; // Net total (including tax)
    private Date date;
    private final User user; // Person that performed sale


    /**
     * Builds a sellable or nonsellable exchange.
     * @param id ID of transaction
     * @param amount Total + tax of transaction
     * @param date Date of transaction
     * @param u User who performed transaction
     */
    public Distribution(String id, double amount, Date date, User u)
    {
        this.ID = id;
        this.user = u;
        this.setDate(date);
        this.setAmount(amount);
    }

    public String getID() {
        return ID;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public User getUser() {
        return user;
    }
}
