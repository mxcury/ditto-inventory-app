
# **Implementing a Search Screen in a Ditto Demo App**


*Build a search screen into an existing Ditto demo app to allow users to search data within a collection*

## **Overview & Initial Thought Process**

For this challenge, I aimed to introduce a search functionality that would allow users to query data within a Ditto collection. My first step was identifying the necessary Android components:

-   **SearchView** for user input
-   **RecyclerView** to display the search results

Initially, I considered embedding the search functionality directly into the main activity. However, I later realized that a dedicated **Search Activity** would be a more modular and scalable approach.

My first idea was to implement client-side filtering on the dataset, but I concluded that leveraging **DQL** would be more efficient, given that this was a data-oriented challenge. This led me to set up a **remote collection using Ditto's Playground**, populate it with sample data, and modify the app to fetch data from this remote collection instead of a locally stored dataset.

To validate the new approach, I first retrieved and displayed **all** data within the search activity, ensuring the remote collection was properly integrated before implementing search functionality.


## **Attempts & Iterations**

### **1\. Querying the Remote Database**

After successfully connecting to the remote database, I experimented with **DQL queries** via Ditto's portal to determine the best way to filter search results. Initially, I found that the **`LIKE` operator** seemed to be the best fit.

However, implementing it in the app led to **multiple errors**. Upon investigation, I discovered that the project was using **Ditto v4.9.3** (as specified in `build.gradle`), whereas newer versions had better query support. Updating Ditto to the latest version seemed like a potential fix.

### **2\. Query Issues & Alternative Solutions**

Even after updating the SDK, the **`LIKE` operator** did not fetch results as expected. After reviewing older documentation, I found that **`contains()`** was a more reliable alternative for substring searches. Implementing this method finally produced the expected search behaviour.

### **3\. Handling Data Updates & Refactoring**

Switching to a remote collection introduced additional issues:

-   The **item count increment/decrement logic** was not updating correctly because my remote implementation stored `count` as an **integer** instead of Ditto's **`MutableCounter`** type.
-   Since `MutableCounter` provides built-in `.increment()` and `.decrement()` methods, I had to **manually retrieve, modify, and update** the document's count value using arithmetic operations.

Additionally, while retrieving documents from the **Big Peer**, I realized that **item IDs were no longer integers but UUIDs (strings)**. This required **refactoring ID-based operations** to use string comparisons instead.

### **4\. Ensuring RecyclerView Updates on First Launch**

A major issue arose where the RecyclerView was not updating properly on the first launch. Initially, I tried delaying the setup until data was available, but this broke the UI. Then, I ensured DittoManager.startDitto() was fully initialized before loading the UI, but this didn't resolve the problem.

The final fix involved modifying the liveQuery observer to always refresh the dataset before handling updates:
```
    liveQuery = query?.observeLocal { docs, event ->
    val itemsFromDitto = parseDocumentsToItemModel(docs)
    itemUpdateListener.setInitial(itemsFromDitto) // Always update the list
  
    when (event) {
      is DittoLiveQueryEvent.Initial -> {} // No extra work needed here
  
      is DittoLiveQueryEvent.Update -> {
        event.updates.forEach { index ->
          val doc = docs[index]
          val count = doc["count"].intValue
          itemUpdateListener.updateCount(index, count)
        }
      }
    }
  }
```

This ensured that the RecyclerView always displayed the latest data while still handling incremental updates properly.
## **Final Solution**

The final implementation used Ditto's **`contains()`** operation to allow substring searches within the `name` and `description` fields:

```
val query = collection?.find(
    "(contains(name, '$sanitizedSearchText') || contains(description, '$sanitizedSearchText'))"
)

```

I also attempted to use Ditto's alternative approach for passing query arguments but couldn't achieve the same behaviour as direct variable substitution.


## **Challenges & Lessons Learned**

### **1\. Constructing Effective Queries in DQL**

I experimented with different approaches to retrieving matching data:

#### **Using `LIKE` Operator (Did not work as expected)**

```
val query = collection?.find(
    "name LIKE '%${sanitizedSearchText}%' OR description LIKE '%${sanitizedSearchText}%'"
)

```

Although `LIKE` seemed optimal, **DQL execution behaviour** made it infeasible for the task.

#### **Using Regular Expressions (Syntax Errors in Ditto DQL)**

```
val query = collection?.find(
    "SELECT * FROM inventories WHERE regexp_like(name, '(?i)$sanitizedSearchText') OR regexp_like(description, '(?i)$sanitizedSearchText')"
)

```

Regular expressions were another potential solution but were **not behaving as intended**.

Ultimately, **`contains()` was the most reliable approach**, despite the concerns with case sensitivity.

### **2\. Handling Remote Data & Mutable Fields**

-   The need to manually update **integer counts** instead of relying on `MutableCounter`.
-   Adjusting ID handling due to a shift from **integer IDs to UUIDs** when using remote collections.


## **Future Improvements**

- **Case-Insensitive Search:** Modify query behaviour to ensure search results **ignore letter case**, allowing more flexible user input.
- **Item Count Validation:** Prevent item counts from dropping below zero by capping values appropriately.

```
if (count < 0) count = 0

```

- **Optimize Query Argument Passing:** Investigate how to pass search parameters more efficiently within DQL queries.
