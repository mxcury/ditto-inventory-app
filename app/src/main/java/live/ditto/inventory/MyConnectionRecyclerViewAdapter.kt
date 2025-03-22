package live.ditto.inventory

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import live.ditto.inventory.R

class Connection(val deviceName: String, val connectionType: String)

class MyConnectionRecyclerViewAdapter: RecyclerView.Adapter<MyConnectionRecyclerViewAdapter.ViewHolder>() {
    private var connections = emptyList<Connection>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_connection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val connection = connections[position]
        holder.deviceNameLabelView.text = "Device Name:"
        holder.deviceNameView.text = connection.deviceName
        holder.connectionTypeLabelView.text = "Connection Type:"
        holder.connectionTypeView.text = connection.connectionType
    }

    override fun getItemCount(): Int = connections.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceNameLabelView: TextView = view.findViewById(R.id.device_name_label)
        val deviceNameView: TextView = view.findViewById(R.id.device_name)
        val connectionTypeLabelView: TextView = view.findViewById(R.id.connection_type_label)
        val connectionTypeView: TextView = view.findViewById(R.id.connection_type)

        override fun toString(): String = super.toString() + " '" + deviceNameView.text + connectionTypeView.text + "'"
    }

    fun updateConnections(connections: List<Connection>) {
        Log.i("@@@", "Connections: " + connections.size.toString())
        this.connections = connections
        notifyDataSetChanged()
    }
}