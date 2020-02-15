package com.charis.data;

public final class Category
{
    private final String ID;
    private String name;


    /**
     * Builds a category.
     * @param id ID of category
     * @param name Name of category
     */
    public Category(String id, String name)
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

    public void setName(String name) {
        this.name = name;
    }
}
