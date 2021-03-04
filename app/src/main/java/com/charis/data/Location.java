package com.charis.data;


/**
 * Encapsulates properties of locations items
 * can be stored at.
 */
public final class Location
{
    private final String ID;
    private String name;


    /**
     * Creates a physical location for items.
     * @param id ID of location
     * @param name Name of location
     */
    public Location(String id, String name)
    {
        this.ID = id;
        this.setName(name);
    }

    public String getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name)
    {
        if(name != null)
            this.name = name;
        else
            this.name = "";
    }
}
