package live.ditto.inventory

import android.content.Context
import android.util.Log
import live.ditto.*
import live.ditto.android.DefaultAndroidDittoDependencies

object DittoManager {

    // Make COLLECTION_NAME public instead of private
    const val COLLECTION_NAME = "inventories"

    // Add a function to access itemsForView
    fun getItemsForView(): List<ItemModel> {
        return itemUpdateListener.getItems();
    }



    /* Interfaces */
    interface ItemUpdateListener {
        fun setInitial(items: List<ItemModel>)
        fun getItems() : List<ItemModel>
        fun updateCount(index: Int, count: Int)
    }


    /* Settable from outside */
    lateinit var itemUpdateListener: ItemUpdateListener


    /* Get-only properties */
    var ditto: Ditto? = null; private set


    /* Private properties */

    private var collection: DittoCollection? = null

    private var subscription: DittoSyncSubscription? = null
    private var liveQuery: DittoLiveQuery? = null

    // Those values should be pasted in 'gradle.properties'. See the notion page for more details.
    private const val APP_ID = BuildConfig.APP_ID
    private const val ONLINE_AUTH_TOKEN = BuildConfig.ONLINE_AUTH_TOKEN


    /* Internal functions and properties */
    internal suspend fun startDitto(context: Context) {
        DittoLogger.minimumLogLevel = DittoLogLevel.DEBUG

        val dependencies = DefaultAndroidDittoDependencies(context)
        ditto = Ditto(dependencies, DittoIdentity.OnlinePlayground(dependencies, APP_ID, ONLINE_AUTH_TOKEN, true))

        try {
            // Disable sync with V3 Ditto
            ditto?.disableSyncWithV3()
            // Disable avoid_redundant_bluetooth
            ditto?.store?.execute("ALTER SYSTEM SET mesh_chooser_avoid_redundant_bluetooth = false")
            ditto?.startSync()
        } catch (e: Exception) {
            Log.e(e.message, e.localizedMessage)
        }

        collection = ditto?.store?.collection(COLLECTION_NAME)

        observeItems()
//        insertDefaultDataIfAbsent()
    }

    internal fun increment(itemId: Int) {
        collection?.findById(itemId)?.update {
            it?.get("counter")?.counter?.increment(1.0)
        }
    }

    internal fun decrement(itemId: Int) {
        collection?.findById(itemId)?.update {
            it?.get("counter")?.counter?.increment(-1.0)
        }
    }

    internal val sdkVersion: String?
        get() = ditto?.sdkVersion


    /* Private functions and properties */

//    private fun insertDefaultDataIfAbsent() {
//
//        ditto?.store?.write { transaction ->
//            val scope = transaction.scoped(COLLECTION_NAME)
//
//            for (viewItem in itemsForView) {
//                val doc = collection?.findById(viewItem.itemId)?.exec()
//
//                if (doc == null) {
//                    scope.upsert(mapOf("_id" to viewItem.itemId, "counter" to DittoCounter()), writeStrategy = DittoWriteStrategy.InsertDefaultIfAbsent)
//                } else {
//                    viewItem.count = doc["counter"].intValue
//                }
//            }
//        }
//    }

    private fun observeItems() {
        val query = collection?.findAll()

        subscription =  ditto?.sync?.registerSubscription(query = "SELECT * FROM inventories")

        liveQuery = query?.observeLocal { docs, event ->

            when (event) {

                is DittoLiveQueryEvent.Initial -> {
                    val itemsFromDitto = parseDocumentsToItemModel(docs)
                    itemUpdateListener.setInitial(itemsFromDitto)
                }

                is DittoLiveQueryEvent.Update -> {
                    event.updates.forEach { index ->
                        val doc = docs[index]
                        val count = doc["counter"].intValue

                        itemUpdateListener.updateCount(index, count)
                    }
                }
            }
        }
    }

//    private val itemsForView = arrayOf(
//        ItemModel(0, R.drawable.coke, "Coca-Cola", 2.50, "A Can of Coca-Cola"),
//        ItemModel(1, R.drawable.drpepper, "Dr. Pepper", 2.50, "A Can of Dr. Pepper"),
//        ItemModel(2,R.drawable.lays, "Lay's Classic", 3.99, "Original Classic Lay's Bag of Chips"),
//        ItemModel(3, R.drawable.brownies, "Brownies", 6.50,"Brownies, Diet Sugar Free Version"),
//        ItemModel(4, R.drawable.blt, "Classic BLT Egg", 2.50, "Contains Egg, Meats and Dairy")
//    )

    fun parseDocumentsToItemModel(docs: List<DittoDocument>): List<ItemModel> {
        return docs.map { doc ->
            ItemModel(
                itemId = doc["_id"].hashCode(),
                image = getImageResource(doc["image"].stringValue),
                title = doc["name"].stringValue,
                price = doc["price"].doubleValue,
                detail = doc["description"].stringValue,
                count = doc["count"].intValue
            )
        }
    }

    fun getImageResource(imageName: String): Int {
        return when (imageName) {
            "coke" -> R.drawable.coke
            "drpepper" -> R.drawable.drpepper
            "lays" -> R.drawable.lays
            "brownies" -> R.drawable.brownies
            "blt" -> R.drawable.blt
            else -> R.drawable.placeholder // Default image
        }
    }
}