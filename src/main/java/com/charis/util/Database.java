package com.charis.util;

import com.charis.data.Category;
import com.charis.data.Distribution;
import com.charis.data.Enum.Condition;
import com.charis.data.Item;
import com.charis.data.Kit;
import com.charis.data.Location;
import com.charis.data.NonSellableItem;
import com.charis.data.SellableItem;
import com.charis.data.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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
     * Closes the connection to the database.
     * @return True if connection closed
     */
    public boolean close()
    {
        Task t = this.db.terminate();
        waitForResponse(t);
        return t.isSuccessful();
    }


    /**
     * Creates a user record with given attributes.
     * Password is saved in the database using bcrypt algorithm
     * with random salt.
     * Password attribute becomes the hashed version in
     * returned User object.
     * @param uname Username
     * @param password plaintext password
     * @param admin Is user an admin?
     * @param active Is account active?
     * @param fname First name
     * @param lname Last name
     * @return User object from given data
     */
    public User createUser(String uname, String password, boolean admin, boolean active, String fname, String lname)
    {
        password = hashPassword(password); // Encrypt password

        HashMap<String, Object> map = new HashMap();
        map.put("password", password);
        map.put("admin", admin);
        map.put("active", active);
        map.put("firstName", fname);
        map.put("lastName", lname);

        DocumentReference ref = getDatabase().collection("User").document(uname.toLowerCase());
        ref.set(map);

        // Leave password blank in object
        return new User(uname, fname, lname, password, admin, active);
    }


    /**
     * Hashes the plaintext password with a random
     * salt using BCrypt.
     * @param pass Plaintext password
     * @return Hashed version of password
     */
    private String hashPassword(String pass)
    {
        String salt = BCrypt.gensalt(10);
        String hashed = BCrypt.hashpw(pass, salt);
        return hashed;
    }


    /**
     * Checks plaintext password against the hashed version
     * from the database to see if they match.
     * @param plain Plaintext password
     * @param hashed Hashed password with salt
     * @return True if plaintext becomes the hashed version
     * when run through the hashing algorithm
     */
    public boolean checkHashedPassword(String plain, String hashed)
    {
        return BCrypt.checkpw(plain, hashed);
    }


    /**
     * Updates a user record with all values in u
     * except for password. Use function updatePassword()
     * to update the password.
     * @param u User object to update
     */
    public boolean updateUser(User u)
    {
        // Overwrite existing document
        DocumentReference ref = getDatabase().collection("User").document(u.getUsername());
        Task t1 = ref.update("firstName", u.getFirstName());
        Task t2 = ref.update("lastName", u.getLastName());
        Task t3 = ref.update("active", u.isActive());
        Task t4 = ref.update("admin", u.isAdmin());

        Task t5 = Tasks.whenAll(t1, t2, t3, t4); // Combine to single task
        waitForResponse(t5);
        return t5.isSuccessful();
    }


    /**
     * Updates the plaintext password saved in u,
     * encrypts it, and saves the hashed password
     * to the database.
     * @param u Object with plaintext password
     * @return True if successful
     */
    public boolean updatePassword(User u)
    {
        if(u == null)
            return false;

        DocumentReference ref = getDatabase().collection("User").document(u.getUsername());
        Task t = ref.update("password", hashPassword(u.getPassword()));
        waitForResponse(t);
        return t.isSuccessful();
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
    private Distribution createDistribution(double amount, Date date, User user)
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
     * Creates a location in the database.
     * @param name Name of location
     * @return Location object
     */
    public Location createLocation(String name)
    {
        HashMap<String, Object> map = new HashMap();
        map.put("name", name);


        Task t = getDatabase().collection("Location").add(map);
        waitForResponse(t);


        DocumentReference ref = (DocumentReference) t.getResult();
        return new Location(ref.getId(), name);
    }


    /**
     * Returns a locatino from the database.
     * @param id ID of location
     * @return Null if read failed
     */
    public Location getLocation(String id)
    {
        Task t = getDatabase().collection("Location").document(id).get();
        waitForResponse(t);

        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            DocumentSnapshot doc = snap.getDocuments().get(0);

            return new Location(id, doc.getString("name"));
        }
        else
            return null;
    }


    /**
     * Update location with given object data.
     * @param loc Location to update
     * @return True if successful
     */
    public boolean updateLocation(Location loc)
    {
        if (loc == null)
            return false;

        DocumentReference ref = getDatabase().collection("Location").document(loc.getID());
        Task t = ref.update("name", loc.getName());
        waitForResponse(t);

        return t.isSuccessful();
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

        Task t = getDatabase().collection("Category").add(map);
        waitForResponse(t);
        DocumentReference ref = (DocumentReference) t.getResult();

        return new Category(ref.getId(), name);
    }


    /**
     * Updates category with attributes in c.
     * @param c Category to update
     */
    public boolean updateCategory(Category c)
    {
        DocumentReference ref = getDatabase().collection("Category").document(c.getID());
        Task t = ref.update("name", c.getName());
        waitForResponse(t);

        return t.isSuccessful();
    }


    /**
     * Returns category with given ID.
     * @param id ID of category
     * @return Null if failed to read
     */
    public Category getCategory(String id)
    {
        Task t = getDatabase().collection("Category").document(id).get();
        waitForResponse(t);

        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            List<DocumentSnapshot> doc = snap.getDocuments();

            String name = doc.get(0).getString("name");
            return new Category(doc.get(0).getId(), name);
        }
        else
            return null;
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
    private boolean createItem(String id, Date rec, String desc, Condition cond, double price, Category cat)
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
    public boolean createSellableItem(String id, Date rec, String desc, Condition cond, double price, Category cat, int quantity, Location loc)
    {
        boolean suc = createItem(id, rec, desc, cond, price, cat); // Make document in Item collection
        if(!suc)
            return false;


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
    public boolean createNonSellableItem(String id, Date rec, String desc, Condition cond, double price, Category cat, String source, int quantity, Location loc)
    {
        boolean suc = createItem(id, rec, desc, cond, price, cat); // Make document in Item collection
        if(!suc)
            return false;


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
     * Returns a nonsellable object from the database
     * with the given ID.
     * @param id Barcode of item
     * @return Null if read failed
     */
    public NonSellableItem getNonSellableItem(String id)
    {
        // Get rest of item info
        DocumentSnapshot itemSnap = getItem(id);
        if(itemSnap == null) /// Check for success
            return null;

        // Pull info from item snapshot
        Date received = itemSnap.getDate("received");
        String desc = itemSnap.getString("description");
        Condition cond = Condition.toCondition(itemSnap.getLong("condition"));
        double amount = itemSnap.getDouble("amount");
        Category cat = getCategory(itemSnap.getString("category"));



        // Pull from NonSellable collection
        Task t = getDatabase().collection("NonSellable").document(id).get();
        waitForResponse(t);


        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            DocumentSnapshot docs = snap.getDocuments().get(0); // Should only return one result

            String source = docs.getString("source");
            int quantity = Integer.valueOf(String.valueOf(docs.getLong("quantity")));
            Location loc = getLocation(docs.getString("location"));

            return new NonSellableItem(id, received, desc, quantity, cond, amount, cat, source, loc);
        }
        else
            return null;
    }


    /**
     * Returns a sellable object from the database
     * with the given ID.
     * @param id Barcode of item
     * @return Null if read failed
     */
    public SellableItem getSellableItem(String id)
    {
        // Get rest of item info
        DocumentSnapshot itemSnap = getItem(id);
        if(itemSnap == null) /// Check for success
            return null;

        // Pull info from item snapshot
        Date received = itemSnap.getDate("received");
        String desc = itemSnap.getString("description");
        Condition cond = Condition.toCondition(itemSnap.getLong("condition"));
        double amount = itemSnap.getDouble("amount");
        Category cat = getCategory(itemSnap.getString("category"));



        // Pull from Sellable collection
        Task t = getDatabase().collection("Sellable").document(id).get();
        waitForResponse(t);


        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            DocumentSnapshot docs = snap.getDocuments().get(0); // Should only return one result

            int quantity = Integer.valueOf(String.valueOf(docs.getLong("quantity")));
            Location loc = getLocation(docs.getString("location"));

            return new SellableItem(id, received, desc, quantity, cond, amount, cat, loc);
        }
        else
            return null;
    }



    /**
     * Updates the quantity of the passed sellable item to
     * the database. Use this function instead of
     * updateItem() to save database read and writes.
     * @param item Item to update
     * @return True if successful
     */
    public boolean updateItemQuantity(SellableItem item)
    {
        if(item == null)
            return false;


        DocumentReference ref = getDatabase().collection("Sellable").document(item.getID());
        Task t = ref.update("quantity", item.getQuantity());

        waitForResponse(t);

        return t.isSuccessful();
    }


    /**
     * Updates the quantity of the passed sellable item to
     * the database. Use this function instead of
     * updateItem() to save database read and writes.
     * @param item Item to update
     * @return True if successful
     */
    public boolean updateItemQuantity(NonSellableItem item)
    {
        if(item == null)
            return false;


        DocumentReference ref = getDatabase().collection("NonSellable").document(item.getID());
        Task t = ref.update("quantity", item.getQuantity());

        waitForResponse(t);

        return t.isSuccessful();
    }


    /**
     * Updates the item with attributes in the object.
     * @param item Item to update
     * @return True if successful update
     */
    private boolean updateItem(Item item)
    {
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

        return (t1.isSuccessful() && t2.isSuccessful() && t3.isSuccessful() && t4.isSuccessful());
    }


    /**
     * Updates the sellable document with ID in item.
     * @param item Item to update
     * @return True if successful
     */
    public boolean updateSellable(Item item)
    {
        if(item == null)
            return false;

        DocumentReference ref = getDatabase().collection("Sellable").document(item.getID());
        Task t1 = ref.update("quantity", item.getQuantity());
        Task t2 = ref.update("location", item.getLocation());

        waitForResponse(t1);
        waitForResponse(t2);

        if (t1.isSuccessful() && t2.isSuccessful())
            return updateItem(item);
        else
            return false;
    }


    /**
     * Updates the nonsellable document with ID in item.
     * @param item Item to update
     * @return True is successful
     */
    public boolean updateNonSellable(NonSellableItem item)
    {
        if(item == null)
            return false;

        DocumentReference ref = getDatabase().collection("NonSellable").document(item.getID());
        Task t1 = ref.update("source", ((NonSellableItem)item).getSource());
        Task t2 = ref.update("quantity", item.getQuantity());
        Task t3 = ref.update("location", item.getLocation());

        waitForResponse(t1);
        waitForResponse(t2);
        waitForResponse(t3);

        if (t1.isSuccessful() && t2.isSuccessful() && t3.isSuccessful())
            return updateItem(item);
        else
            return false;
    }


    /**
     * Create a distribution of sellable items in the database.
     * item and quantity are arrays of items and the number of each item sold
     * and should be ordered with matching indices.
     * @param item Array of items in distribution
     * @param quantity Array of quantities of each item sold
     * @param amount Total + tax of sale
     * @param date Date of sale
     * @param user User who performed sale
     * @return True if successful
     */
    public boolean createDistItemRelation(SellableItem[] item, int[] quantity, double amount, Date date, User user)
    {
        if(item.length != quantity.length) // Stop if arrays are different lengths
            return false;


        Distribution dist = createDistribution(amount, date, user); // Make distribution
        if(dist == null)
            return false;

        boolean suc;
        for(int i = 0; i < item.length; i++)
        {
            HashMap<String, Object> map = new HashMap();
            map.put("item", item[i].getID());
            map.put("dist", dist.getID());
            map.put("quantity", quantity[i]);

            // Create dist_item
            String doc = dist.getID() + "_" + item[i].getID();
            DocumentReference ref = getDatabase().collection("Dist_Item").document(doc);
            ref.set(map);

            // Update quantity in stock
            item[i].decreaseQuantity(quantity[i]);
            suc = updateItemQuantity(item[i]);

            if(!suc) // If not successful save
                return false;
        }

        return true;
    }


    /**
     * Create a distribution of nonsellable items in the database.
     * item and quantity are arrays of items and the number of each item sold
     * and should be ordered with matching indices.
     * @param item Array of items in distribution
     * @param quantity Array of quantities of each item sold
     * @param amount Total + tax of sale
     * @param date Date of sale
     * @param user User who performed sale
     * @return True if successful
     */
    public boolean createDistItemRelation(NonSellableItem[] item, int[] quantity, double amount, Date date, User user)
    {
        if(item.length != quantity.length) // Stop if arrays are different lengths
            return false;


        Distribution dist = createDistribution(amount, date, user); // Make distribution
        if(dist == null)
            return false;

        boolean suc;
        for(int i = 0; i < item.length; i++)
        {
            HashMap<String, Object> map = new HashMap();
            map.put("item", item[i].getID());
            map.put("dist", dist.getID());
            map.put("quantity", quantity[i]);

            // Create dist_item
            String doc = dist.getID() + "_" + item[i].getID();
            DocumentReference ref = getDatabase().collection("Dist_Item").document(doc);
            ref.set(map);

            // Update quantity in stock
            item[i].decreaseQuantity(quantity[i]);
            suc = updateItemQuantity(item[i]);

            if(!suc) // If not successful save
                return false;
        }

        return true;
    }


    /**
     * Creates a kit in the database.
     * @param id Barcode for kit
     * @param name Name for kit
     * @param descr Description for kit
     * @return Null object if save failed
     */
    public Kit createKit(String id, String name, String descr)
    {
        HashMap<String, Object> map = new HashMap();
        map.put("ID", id);
        map.put("name", name);
        map.put("description", descr);

        DocumentReference ref = getDatabase().collection("Kit").document(id);
        Task t = ref.set(map);

        waitForResponse(t);

        return new Kit(id, name, descr);
    }


    /**
     * Returns the nonsellable items in a given kit.
     * No items in kit will return an empty array.
     * @param kit Kit to search
     * @return Array of items in kit
     */
    public NonSellableItem[] getItemsFromKit(Kit kit)
    {
        NonSellableItem[] items;

        Task t = getDatabase().collection("Kit_Item").whereEqualTo("kit", kit.getID()).get();
        waitForResponse(t);

        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            List<DocumentSnapshot> docs = snap.getDocuments();
            items = new NonSellableItem[docs.size()];

            for(int i = 0; i < items.length; i++)
            {
                items[i] = getNonSellableItem(docs.get(i).getString("ID"));
            }
        }
        else
            items = new NonSellableItem[0];

        return items;
    }



    /**
     * Returns reference of the item document of ID.
     * @param id Document ID
     * @return DocumentReference to document
     */
    private DocumentSnapshot getItem(String id)
    {
        Task t = getDatabase().collection("Item").whereEqualTo("ID", id).get();
        waitForResponse(t);

        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            List<DocumentSnapshot> list = snap.getDocuments();
            return list.get(0);
        }

        return null;
    }


    /**
     * Assign the items that belong to a kit.
     * Using this function will erase all previous nonsellable
     * items assigned to the kit.
     * Indices in items and quantities should be parallel.
     * @param kit Kit to add items to
     * @param items Array of items to add to kit
     * @param quantity Array of quantities of each item in var items
     * @return True on success of save
     */
    public boolean createKitItemRelation(Kit kit, NonSellableItem[] items, int[] quantity)
    {
        if(items.length != quantity.length) // Must be same lengths
            return false;


        // Delete all relations involving kit
        boolean suc = deleteKitItemRelation(kit);
        if(!suc) // If delete failed
            return false;

        // Create kit/item relation
        for(int i = 0; i < items.length; i++)
        {
            suc = createKitItem(kit, items[i], quantity[i]);

            if(!suc) // if save failed
                return false;
        }


        return true;
    }


    /**
     * Creates the kit/item relation in the Kit_Item collection.
     * Document name is KitID_ItemID.
     * @param kit Kit to relate
     * @param item Item to relate
     * @param quantity Quantity to save
     * @return True if save successful
     */
    private boolean createKitItem(Kit kit, Item item, int quantity)
    {
        HashMap<String, Object> map = new HashMap();
        map.put("item", item.getID());
        map.put("kit", kit.getID());
        map.put("quantity", quantity);

        String id = kit.getID() + "_" + item.getID();
        DocumentReference ref = getDatabase().collection("Kit_Item").document(id);
        Task t = ref.set(map);
        waitForResponse(t);

        return t.isSuccessful();
    }


    /**
     * Deletes records involving given kit ID from database.
     * @param kit Kit records to delete
     * @return True if successful
     */
    private boolean deleteKitItemRelation(Kit kit)
    {
        // Get all records with kit ID in it
        Task t = getDatabase().collection("Kit").whereEqualTo("kit", kit.getID()).get();
        waitForResponse(t);


        // Erase all previous documents for specified kit
        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            List<DocumentSnapshot> docs = snap.getDocuments();

            for(int i = 0; i < docs.size(); i++)
            {
                Task t1 = docs.get(i).getReference().delete(); // Delete document
                waitForResponse(t1); // Could be faster with task monitor?

                if(!t1.isSuccessful())
                    return false;
            }
        }
        else
            return false; // Not successful


        return true;
    }


    /**
     * Loops waiting for the task to complete.
     * @param t Thread task
     */
    private void waitForResponse(Task t)
    {
        while(!t.isComplete())
        {
            /*try { Tasks.await(t, 10, TimeUnit.MILLISECONDS); } // Max 20ms wait time
            catch (InterruptedException e) { e.printStackTrace(); }
            catch (TimeoutException e) { e.printStackTrace(); }
            catch (ExecutionException e) { e.printStackTrace(); }*/
        }
    }
}
