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


import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public final class Database
{
    /**
     * Firestore access object
     */
    final private FirebaseFirestore db;

    /**
     * The length of barcode IDs.
     */
    final static public int BARCODE_SIZE = 13;


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
        String pass = hashPassword(password); // Encrypt password

        HashMap<String, Object> map = new HashMap();
        map.put("password", hashPassword(password));
        map.put("admin", admin);
        map.put("active", active);
        map.put("firstName", fname);
        map.put("lastName", lname);

        DocumentReference ref = getDatabase().collection("User").document(uname.toLowerCase());
        ref.set(map);

        // Leave password blank in object
        return new User(uname, fname, lname, pass, admin, active);
    }


    /**
     * Hashes the plaintext password with a random
     * salt using BCrypt.
     * @param pass Plaintext password
     * @return Hashed version of password
     */
    private String hashPassword(String pass)
    {
        String salt = BCrypt.gensalt(5);
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
            DocumentSnapshot doc = (DocumentSnapshot) t.getResult();

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
     * Returns an array of all locations in the database.
     * Will never return null. If no locations are found,
     * it returns an array of zero length.
     * @return Array of locations
     */
    public Location[] getAllLocations()
    {
        Location[] locs;
        Task t = getDatabase().collection("Location").get();
        waitForResponse(t);

        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            List<DocumentSnapshot> docs = snap.getDocuments();
            locs = new Location[docs.size()];

            for(int i = 0; i < locs.length; i++)
            {
                String id = docs.get(i).getId();
                String name = docs.get(i).getString("name");

                locs[i] = new Location(id, name);
            }
        }
        else
            locs = new Location[0];

        return locs;
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
            DocumentSnapshot doc = (DocumentSnapshot) t.getResult();

            String name = doc.getString("name");
            return new Category(id, name);
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
     * ID of the item is generated.
     * @param rec Date received
     * @param desc Additional description
     * @param cond Condition of item
     * @param price Price of item
     * @param cat Category of item
     * @param quantity Quantity in stock
     * @param loc Location of item
     * @return True if successful
     */
    public boolean createSellableItem(Date rec, String desc, Condition cond, double price, Category cat, int quantity, Location loc)
    {
        String id = makeID(); // Generate an ID
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
     * ID is generated for item.
     * @param rec Date received
     * @param desc Additional description
     * @param cond Condition of item
     * @param price Price of item
     * @param cat Category of item
     * @param source Source person/business of item
     * @param quantity Quantity of item
     * @param loc Location of item
     * @return True if successful
     */
    public boolean createNonSellableItem(Date rec, String desc, Condition cond, double price, Category cat, String source, int quantity, Location loc)
    {
        String id = makeID(); // Generate an ID
        boolean suc = createItem(id, rec, desc, cond, price, cat); // Make document in Item collection
        if(!suc)
            return false;


        HashMap<String, Object> map = new HashMap();
        map.put("source", source);
        map.put("quantity", quantity);
        map.put("location", loc.getID());

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
        double amount = itemSnap.getDouble("price");
        Category cat = getCategory(itemSnap.getString("category"));



        // Pull from NonSellable collection
        Task t = getDatabase().collection("NonSellable").document(id).get();
        waitForResponse(t);


        if(t.isSuccessful())
        {
            DocumentSnapshot docs = (DocumentSnapshot) t.getResult();
            if(docs.exists())
            {
                String source = docs.getString("source");
                int quantity = Integer.parseInt(String.valueOf(docs.getLong("quantity")));
                Location loc = getLocation(docs.getString("location"));

                return new NonSellableItem(id, received, desc, quantity, cond, amount, cat, source, loc);
            }
        }

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
        double amount = itemSnap.getDouble("price");
        Category cat = getCategory(itemSnap.getString("category"));



        // Pull from Sellable collection
        Task t = getDatabase().collection("Sellable").document(id).get();
        waitForResponse(t);


        if(t.isSuccessful())
        {
            DocumentSnapshot docs = (DocumentSnapshot) t.getResult();
            if(docs.exists())
            {
                int quantity = Integer.parseInt(String.valueOf(docs.getLong("quantity")));
                Location loc = getLocation(docs.getString("location"));

                return new SellableItem(id, received, desc, quantity, cond, amount, cat, loc);
            }
        }

        return null;
    }


    /**
     * Generate a random ID of length
     * BARCODE_SIZE consisting of integers.
     * @return Random ID
     */
    private String makeID()
    {
        Random rand = new Random();
        int[] id = new int[BARCODE_SIZE];

        for(int i = 0; i < BARCODE_SIZE; i++)
        {
            id[i] = rand.nextInt(10); // [0, 10)
        }

        return Arrays.toString(id).replaceAll("\\[|\\]|,|\\s", "");
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
    public boolean updateSellable(SellableItem item)
    {
        if(item == null)
            return false;

        DocumentReference ref = getDatabase().collection("Sellable").document(item.getID());
        Task t1 = ref.update("quantity", item.getQuantity());
        Task t2 = ref.update("location", item.getLocation().getID());

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
        Task t3 = ref.update("location", item.getLocation().getID());

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
     * @param sell Array of items in distribution
     * @param sellQuant Array of quantities of each item sold
     * @param nonSell Array of items in distribution
     * @param nonSellQuant Array of quantities of each item sold
     * @param amount Total + tax of sale
     * @param date Date of sale
     * @param user User who performed sale
     * @return True if successful
     */
    public boolean createDistItemRelation(SellableItem[] sell, int[] sellQuant, NonSellableItem[] nonSell, int[] nonSellQuant, double amount, Date date, User user)
    {
        if((sell.length != sellQuant.length) || (nonSell.length != nonSellQuant.length)) // Stop if arrays are different lengths
            return false;


        Distribution dist = createDistribution(amount, date, user); // Make distribution
        if(dist == null)
            return false;

        // Save NonSellables
        for(int i = 0; i < nonSell.length; i++)
        {
            boolean suc = saveDistItem(nonSell, nonSellQuant, dist.getID());

            if(!suc)
                return false;
        }

        // Save Sellables
        for(int i = 0; i < sell.length; i++)
        {
            boolean suc = saveDistItem(sell, sellQuant, dist.getID());

            if(!suc)
                return false;
        }

        return true;
    }


    /**
     * Saves Sellable and NonSellable items to the database.
     * @param item Array of items
     * @param quantity Array of quantities
     * @param id ID of the distribution
     * @return True if all saves are successful
     */
    private boolean saveDistItem(Item[] item, int[] quantity, String id)
    {
        boolean suc = false;
        HashMap<String, Object> map;
        for(int i = 0; i < item.length; i++)
        {
            map = new HashMap();
            map.put("item", item[i].getID());
            map.put("dist", id);
            map.put("quantity", quantity[i]);

            // Create dist_item
            DocumentReference ref = getDatabase().collection("Dist_Item").document();
            ref.set(map);

            item[i].decreaseQuantity(quantity[i]);

            if(item[i] instanceof NonSellableItem)
                suc = updateItemQuantity((NonSellableItem) item[i]);
            else
                suc = updateItemQuantity((SellableItem) item[i]);

            if(!suc) // If not successful save
                return false;
        }

        return suc;
    }



    /**
     * Creates a kit in the database. ID is automatically
     * generated to a 13 character barcode.
     * @param name Name for kit
     * @param descr Description for kit
     * @return Null object if save failed
     */
    private Kit createKit(String name, String descr)
    {
        String id = this.makeID();

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
     * Returns the items in a given kit.
     * No items in kit will return an empty array.
     * @param kit Kit to search
     * @return Array of items in kit
     */
    public Item[] getItemsFromKit(Kit kit)
    {
        Item[] items;

        Task t = getDatabase().collection("Kit_Item").whereEqualTo("kit", kit.getID()).get();
        waitForResponse(t);

        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            List<DocumentSnapshot> docs = snap.getDocuments();
            items = new Item[docs.size()];

            for(int i = 0; i < items.length; i++)
            {
                if(docs.get(i).getBoolean("sellable"))
                    items[i] = getSellableItem(docs.get(i).getString("item"));
                else
                    items[i] = getNonSellableItem(docs.get(i).getString("item"));

                // Set quantity
                long quant = docs.get(i).getLong("quantity");
                items[i].setQuantity(Integer.parseInt(String.valueOf(quant)));
            }
        }
        else
            items = new Item[0];

        return items;
    }




    /**
     * Saves a kit and its associated items.  If the kit does not exist,
     * a new one will be created. Then,
     * The item relations with quantities are updated
     * or created.
     * @param kit Kit to use - Null for new kit
     * @param name Name of kit
     * @param desc Description of kit
     * @param items List of items to put in kit
     * @param sellable List of item sellable types. i.e. True for SellableItem type.
     * @param quantity List of quantities
     * @return Returns true if the function is executed, not
     * if the saves were successful.
     */
    public boolean saveKit(Kit kit, String name, String desc, Item[] items, boolean[] sellable, int[] quantity)
    {
        if(items == null || sellable == null || quantity == null)
            return false;

        if(items.length != sellable.length && items.length != quantity.length)
            return false;

        // Does kit exist?
        if(kit == null || kit.getID().length() != BARCODE_SIZE) // Check if barcode is right
        {
            kit = createKit(name, desc); // Make the new kit
        }
        else // Update Kit information
        {
            updateKit(kit.getID(), name, desc);
        }

        deleteAllKitItemRelation(kit.getID()); // Delete kit/item relations

        // Save kit items
        for(int i = 0 ; i < items.length; i++)
        {
            createKitItem(kit.getID(), items[i].getID(), quantity[i], sellable[i]);
            /*DocumentReference ref = isKitItemExist(kit.getID(), items[i].getID());
            if(ref == null) // Relation doesn't exist
            {
                createKitItem(kit.getID(), items[i].getID(), quantity[i], sellable[i]);
            }
            else // Update relation attributes
            {
                ref.update("quantity", quantity[i]);
            }*/
        }

        return true;
    }



    /**
     * Update the name and description of a kit
     * with the given ID. Update will fail
     * if the kit doesn't exist.
     * @param id ID of kit
     * @param name Name of kit
     * @param desc Description of kit
     */
    private void updateKit(String id, String name, String desc)
    {
        DocumentReference ref = getDatabase().collection("Kit").document(id);
        ref.update("name", name);
        ref.update("description", desc);
    }



    /**
     * Returns a document reference if the document exists,
     * otherwise, null is returned.
     * @param kitID ID of kit
     * @param itemID ID of item
     * @return Document reference or null
     */
    private DocumentReference isKitItemExist(String kitID, String itemID)
    {
        Task t = getDatabase().collection("Kit_Item").whereEqualTo("kit", kitID).whereEqualTo("item", itemID).get();
        waitForResponse(t);

        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            List<DocumentSnapshot> docs = snap.getDocuments();

            if(docs.size() > 0)
            {
                return docs.get(0).getReference();
            }
        }

        return null;
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
            if(list.size() > 0)
                return list.get(0);
        }

        return null;
    }



    /**
     * Creates the kit/item relation in the Kit_Item collection.
     * @param kit Kit ID to relate
     * @param item Item ID to relate
     * @param quantity Quantity to save
     */
    private void createKitItem(String kit, String item, int quantity, boolean sellable)
    {
        HashMap<String, Object> map = new HashMap();
        map.put("item", item);
        map.put("kit", kit);
        map.put("quantity", quantity);
        map.put("sellable", sellable);

        DocumentReference ref = getDatabase().collection("Kit_Item").document();
        ref.set(map);
    }


    /**
     * Deletes all kit/item relations from the database.
     * @param kitID ID for the kit
     * @return True if all deletes succeeded
     */
    public boolean deleteAllKitItemRelation(String kitID)
    {
        // Get all records with kit ID in it
        Task t = getDatabase().collection("Kit_Item").whereEqualTo("kit", kitID).get();
        waitForResponse(t);


        // Erase all previous documents for specified kit
        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            List<DocumentSnapshot> docs = snap.getDocuments();

            if(docs.size() > 0)
            {
                for(int i = 0; i < docs.size(); i++)
                {
                    Task t1 = docs.get(i).getReference().delete(); // Delete document
                    waitForResponse(t1);
                    if(!t1.isSuccessful())
                        return false;
                }

                return true;
            }
        }

        return false;
    }


    /**
     * Returns an array of all the kits
     * in the database. Will never be null.
     * If no kits exist, function will return
     * a zero length array.
     * @return Array of Kit
     */
    public Kit[] getAllKits()
    {
        Kit[] kits;

        Task t = getDatabase().collection("Kit").get(); // Get all kits
        waitForResponse(t);

        if(t.isSuccessful())
        {
            QuerySnapshot snap = (QuerySnapshot) t.getResult();
            List<DocumentSnapshot> docs = snap.getDocuments();
            kits = new Kit[docs.size()];

            for(int i = 0; i < kits.length; i++)
            {
                String id = docs.get(i).getString("ID");
                String name = docs.get(i).getString("name");
                String desc = docs.get(i).getString("description");

                kits[i] = new Kit(id, name, desc);
            }
        }
        else
            kits = new Kit[0];

        return kits;
    }


    /**
     * Loops waiting for the task to complete.
     * @param t Thread task
     */
    private void waitForResponse(Task t)
    {
        // Spin lock for now
        while(!t.isComplete())
        {
            /*try { Tasks.await(t, 10, TimeUnit.MILLISECONDS); } // Max 20ms wait time
            catch (InterruptedException e) { e.printStackTrace(); }
            catch (TimeoutException e) { e.printStackTrace(); }
            catch (ExecutionException e) { e.printStackTrace(); }*/
        }
    }
}
