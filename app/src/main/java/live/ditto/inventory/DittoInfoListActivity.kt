package live.ditto.inventory

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.pm.PackageInfoCompat

class DittoInfoListActivity : AppCompatActivity() {

    companion object {
        fun createIntent(context: Context, sdkInfo: String): Intent {
            return Intent(context, DittoInfoListActivity::class.java).apply {
                putExtra("sdkInfo", sdkInfo)
            }
        }
    }

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ditto_info_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Ditto Info"

        val info = applicationContext.packageManager.getPackageInfo(application.packageName, PackageManager.GET_ACTIVITIES)
        val version = PackageInfoCompat.getLongVersionCode(info).toString()
        val appVersionTextView = findViewById<TextView>(R.id.app_version_text_view)
        appVersionTextView.text = "App Version: $version"

        listView = findViewById(R.id.ditto_info_list_view)
        val items = arrayOf("Ditto SDK Info")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                val sdkInfo = intent.getStringExtra("sdkInfo")!!
                val intent = DittoSDKInfoActivity.createIntent(this, sdkInfo)
                startActivity(intent)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onBackPressed()
        return true
    }
}