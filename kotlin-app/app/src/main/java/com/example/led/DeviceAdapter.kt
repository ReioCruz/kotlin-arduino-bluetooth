package com.example.led

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.led.databinding.DeviceListItemBinding

class DeviceAdapter (context: Activity, items: ArrayList<Device>): BaseAdapter() {

    private lateinit var binding: DeviceListItemBinding
    private val context: Activity
    private val items: ArrayList<Device>

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Device {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        binding = DeviceListItemBinding.inflate(context.layoutInflater)

        val view = binding.root
        val nameTextView: TextView = binding.name
        val macAddressTextView: TextView = binding.macAddress

        val currentItem = getItem(position) as Device
        nameTextView.text = currentItem.name
        macAddressTextView.text = currentItem.macAddress

        return view
    }

    init {
        this.context = context
        this.items = items
    }
}