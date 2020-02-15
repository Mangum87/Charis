package com.charis.util;

import com.charis.data.Category;
import com.charis.data.Distribution;
import com.charis.data.Enum.Condition;
import com.charis.data.Item;
import com.charis.data.Location;
import com.charis.data.NonSellableItem;
import com.charis.data.SellableItem;
import com.charis.data.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public final class Database
{
    final private FirebaseFirestore db;


    /**
     * Creates a connection to firestore database.
     */
    public Database()
    {
        this.db = FirebaseFirestore.getInstance();
    }


    /**
     * Returns the database instance.
     * @return Database object
     */
    public FirebaseFirestore getDatabase()
    {
        return this.db;
    }


    /**
     * Creates a user record with given attributes.
     * @param uname Username
     * @param password password
     * @param admin Is user an admin?
     * @param active Is account active?
     * @param fname First name
     * @param lname Last name
     * @return User object from given data
     */
    public User createUser(String uname, String password, boolean admin, boolean active, String fname, String lname)
    {
        HashMap<String, Object> map = new HashMap();
        map.put("password", password);
        map.put("admin", admin);
        map.put("active", active);
        map.put("firstName", fname);
        map.put("lastName", lname);

        DocumentReference ref = getDatabase().collection("User").document(uname.toLowerCase());
        ref.set(map);

        return new User(uname, fname, lname, password, admin, active);
    }


    /**
     * Updates a user record with all values in u.
     * @param u User object to update
     */
    public void updateUser(User u)
    {
        // Overwrite existing document
        createUser(u.getUsername(), u.getPassword(), u.isAdmin(), u.isActive(), u.getFirstName(), u.getLastName());
    }


    /**
     * Retrieve user with given username.
     * @param uname Username of user
     * @return User object from database. Null if not found.
     */
    public User getUser(String uname)
    {
        User user = null;
        DocumentReference ref = getDatabase().collection("User").document(uname.toLowerCase());
        Task t = ref.get();
        waitForResponse(t); // Wait for data

        if(t.isSuccessful()) // Was successful?
        {
            DocumentSnapshot doc = (DocumentSnapshot) t.getResult();

            if(doc.exists()) // Does it exist?
            {
                String fname = doc.getString("firstName");
                String lname = doc.getString("lastName");
                String password = doc.getString("password");
                boolean admin = doc.getBoolean("admin");
                boolean active = doc.getBoolean("active");

                user = new User(uname, fname, lname, password, admin, active);
            }
        }

        return user;
    }


    /**
     * Create a distribution in the database.
     * @param amount Total + tax of transaction
     * @param date Date of transaction
     * @param user User who performed transaction
     * @return Distribution object with given data
     */
    public Distribution createDistribution(double amount, Date date, User user)
    {
        HashMap<String, Object> map = new HashMap();
        map.put("amount", amount);
        map.put("date", new Timestamp(date));
        map.put("user", user.getUsername());

        Task t = getDatabase().collection("Distribution").add(map);
        waitForResponse(t);
        DocumentReference ref = (DocumentReference) t.getResult();

        return new Distribution(ref.getId(), amount, date, user);
    }



    /**
     * Returns all distributions in month and year of given variables.
     * Month uses 0-11 for Jan-Dec.
     * Will return an array with no elements if no
     * records are found.
     * @param month Month of records needed
     * @param year Year of records needed
     * @return Array of Distribution objects in month, may be empty
     */
    public Distribution[] getDistributionsByDate(int month, int year)
    {
        // Make calendars
        Calendar cal = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();

        // Set first date
        cal.set(year, month, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Set second date
        cal2.set(year, month + 1, 1, 0, 0, 0);
        cal2.set(Calendar.MILLISECOND, 0);

        Date d1 = cal.getTime();
        Date d2 = cal2.getTime();

        return getDistributions(cal.getTime(), cal2.getTime());
    }


    /**
     * Pull information from the database with formatted time t1 and t2.
     * @param t1 Beginning date
     * @param t2 Ending date
     * @return Array of Distributions
     */
    private Distribution[] getDistributions(Date t1, Date t2)
    {
        Distribution[] dists;
        CollectionReference ref = getDatabase().collection("Distribution");

        Task t = ref.whereGreaterThanOrEqualTo("date", new Timestamp(t1)).whereLessThan("date", new Timestamp(t2)).get();
        waitForResponse(t);

        if(t.isSuccessful())
        {
            QuerySnapshot q = (QuerySnapshot) t.getResult();

            List<DocumentSnapshot> list = q.getDocuments();
            dists = new Distribution[list.size()];

            for(int i = 0; i < dists.length; i++)
            {
                String id = list.get(i).getId();
                double amount = list.get(i).getDouble("amount");
                //Date date = new Date(list.get(i).getTimestamp("date").getSeconds());
                Date date = list.get(i).getDate("date");
                User u = getUser(list.get(i).getString("user"));

                dists[i] = new Distribution(id, amount, date, u);
            }
        }
        else // Default empty list
            dists = new Distribution[0];

        return dists;
    }


    /**
     * Creates a category record in the database.
     * @param name Name of category
     * @return Category object with given data
     */
    public Category createCategory(String name)
    {
        HashMap<String, Object> map = new HashMap();
        map.put("name", name);

        Task t = getDatabase().collection("Item").add(map);
        waitForResponse(t);
        DocumentReference ref = (DocumentReference) t.getResult();

        return new Category(ref.getId(), name);
    }


    /**
     * Updates category with attributes in c.
     * @param c Category to update
     */
    public void updateCategory(Category c)
    {
        DocumentReference ref = getDatabase().collection("Category").document(c.getID());
        ref.update("name", c.getName());
    }


    /**
     * Returns an array of all categories in the database.
     * @return Array of categories or empty array if no categories exist
     */
    public Category[] getAllCategories()
    {
        Category[] cats;
        Task t = getDatabase().collection("Category").get();
        waitForResponse(t);

        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            List<DocumentSnapshot> docs = snap.getDocuments();
            cats = new Category[docs.size()];

            for(int i = 0; i < cats.length; i++)
            {
                String id = docs.get(i).getId();
                String name = docs.get(i).getString("name");

                cats[i] = new Category(id, name);
            }
        }
        else
            cats = new Category[0];

        return cats;
    }


    /**
     * Creates an item in the database.
     * @param id ID of item
     * @param rec Date received
     * @param desc Additional description
     * @param cond Condition of item
     * @param price Price of item
     * @param cat Category of item
     */
    public boolean createItem(String id, Date rec, String desc, Condition cond, double price, Category cat)
    {
        HashMap<String, Object> map = new HashMap();
        map.put("ID", id);
        map.put("received", new Timestamp(rec));
        map.put("description", desc);
        map.put("condition", Condition.toInt(cond));
        map.put("price", price);
        map.put("category", cat.getID());

        Task t = getDatabase().collection("Item").document(id).set(map);
        waitForResponse(t);

        return t.isSuccessful();
    }


    /**
     * Creates a sellable item in the database.
     * @param id ID of item
     * @param quantity Quantity in stock
     * @param loc Location of item
     * @return True if successful
     */
    public boolean createSellableItem(String id, int quantity, Location loc)
    {
        HashMap<String, Object> map = new HashMap();
        map.put("quantity", quantity);
        map.put("location", loc.getID());

        DocumentReference ref = getDatabase().collection("Sellable").document(id);
        Task t = ref.set(map);
        waitForResponse(t);

        return t.isSuccessful();
    }


    /**
     * Creates a nonsellable item in the database.
     * @param id ID of item
     * @param source Source person/business of item
     * @param quantity Quantity of item
     * @param loc Location of item
     * @return True if successful
     */
    public boolean createNonSellableItem(String id, String source, int quantity, Location loc)
    {
        HashMap<String, Object> map = new HashMap();
        map.put("source", source);
        map.put("quantity", quantity);
        map.put("location", loc);

        DocumentReference ref = getDatabase().collection("NonSellable").document(id);
        Task t = ref.set(map);
        waitForResponse(t);

        return t.isSuccessful();
    }


    /**
     * Updates the item with attributes in the object.
     * @param item Item to update
     * @return True if successful update
     */
    public boolean updateItem(Item item)
    {
        if(item == null)
            return false;


        DocumentReference ref = getDatabase().collection("Item").document(item.getID());
        Task t1 = ref.update("received", new Timestamp(item.getReceived()));
        Task t2 = ref.update("description", item.getDescription());
        Task t3 = ref.update("condition", Condition.toInt(item.getCondition()));
        Task t4 = ref.update("price", item.getPrice());

        // Wait for all tasks to complete
        waitForResponse(t1);
        waitForResponse(t2);
        waitForResponse(t3);
        waitForResponse(t4);

        // Update subclasses
        if(item instanceof SellableItem)
            return updateSellable(item);
        else
            return updateNonSellable(item);
    }


    /**
     * Updates the sellable document.
     * @param item Item to update
     * @return True if successful
     */
    private boolean updateSellable(Item item)
    {
        DocumentReference ref = getDatabase().collection("Sellable").document(item.getID());
        Task t1 = ref.update("quantity", item.getQuantity());
        Task t2 = ref.update("location", item.getLocation());

        waitForResponse(t1);
        waitForResponse(t2);

        return (t1.isSuccessful() && t2.isSuccessful());
    }


    /**
     * Updates the nonsellable document.
     * @param item Item to update
     * @return True is successful
     */
    private boolean updateNonSellable(Item item)
    {
        DocumentReference ref = getDatabase().collection("NonSellable").document(item.getID());
        Task t1 = ref.update("source", ((NonSellableItem)item).getSource());
        Task t2 = ref.update("quantity", item.getQuantity());
        Task t3 = ref.update("location", item.getLocation());

        waitForResponse(t1);
        waitForResponse(t2);
        waitForResponse(t3);

        return (t1.isSuccessful() && t2.isSuccessful() && t3.isSuccessful());
    }


    /**
     * Loops waiting for the task to complete.
     * @param t Thread task
     */
    private void waitForResponse(Task t)
    {
        while(!t.isComplete())
        {
            /*try { t.wait(20); } // Max 20ms wait time
            catch (InterruptedException e) { e.printStackTrace(); }*/
        }
    }
}
