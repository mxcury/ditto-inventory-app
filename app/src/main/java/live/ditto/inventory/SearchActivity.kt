package live.ditto.inventory

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import live.ditto.DittoDocument
import live.ditto.DittoLiveQuery
import live.ditto.DittoLiveQueryEvent
import live.ditto.inventory.DittoManager.parseDocumentsToItemModel

class SearchActivity : AppCompatActivity() {
    private lateinit var searchView: SearchView
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var noResultsTextView: TextView
    private lateinit var itemsAdapter: ItemsAdapter
    private var liveQuery: DittoLiveQuery? = null
    private val searchHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Search Inventory"

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        searchView = findViewById(R.id.search_view)
        resultsRecyclerView = findViewById(R.id.results_recycler_view)
        noResultsTextView = findViewById(R.id.no_results_text_view)

        itemsAdapter = ItemsAdapter()
        resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = itemsAdapter
            addItemDecoration(DividerItemDecoration(this@SearchActivity, DividerItemDecoration.VERTICAL))
        }

        // Set up click listeners for the adapter
        itemsAdapter.onPlusClick = { item ->
            DittoManager.increment(item.itemId)
        }

        itemsAdapter.onMinusClick = { item ->
            DittoManager.decrement(item.itemId)
        }
    }

    private fun setupListeners() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("DittoTechChallenge", "Query submitted: $query")
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d("DittoTechChallenge", "Query changed: $newText")
                performSearch(newText)
                return true
            }
        })
    }

    private fun performSearch(searchText: String? = "") {
        Log.d("SearchActivity", "Performing search with query: $searchText")

        // Cancel any existing live query
        liveQuery?.close()

        val collection = DittoManager.ditto?.store?.collection("inventories")

        val sanitizedSearchText = searchText?.replace("\"", "\\\"") ?: ""

        val query = collection?.find(
            "(contains(name, '$sanitizedSearchText') || contains(description, '$sanitizedSearchText'))"
        )

        try {
            liveQuery = query?.observeLocal { docs, event ->
                docs.forEach { doc ->
                    Log.d("Ditto", "🔍 Raw Document: ${doc.toString()}")
                }
                updateSearchResults(docs)
            }
        } catch (e: Exception) {
            Log.e("SearchActivity", "Error executing query: ${e.message}", e)
            // Show error message to the user
            runOnUiThread {
                noResultsTextView.text = "Error searching: ${e.message}"
                noResultsTextView.visibility = View.VISIBLE
                resultsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun updateSearchResults(docs: List<DittoDocument>) {
        Log.d("SearchActivity", "Updating search results. Document count: ${docs.size}")

        val matchingItems = parseDocumentsToItemModel(docs)

        runOnUiThread {
            if (matchingItems.isEmpty()) {
                noResultsTextView.visibility = View.VISIBLE
                resultsRecyclerView.visibility = View.GONE
            } else {
                noResultsTextView.visibility = View.GONE
                resultsRecyclerView.visibility = View.VISIBLE
                itemsAdapter.setItems(matchingItems)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up the live query when the activity is destroyed
        liveQuery?.close()
    }
}
