package com.charis.data;


/**
 * Encapsulates properties of kits to store
 * multiple items.
 */
public final class Kit
{
    private final String ID;
    private String name;
    private String description;


    /**
     * Creates a kit object.
     * @param id Barcode for kit
     * @param name Name of kit
     * @param desc Additional description of kit
     */
    public Kit(String id, String name, String desc)
    {
        this.ID = id;
        setName(name);
        setDescription(desc);
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description)
    {
        if(description != null)
            this.description = description;
        else
            this.description = "";
    }
}
