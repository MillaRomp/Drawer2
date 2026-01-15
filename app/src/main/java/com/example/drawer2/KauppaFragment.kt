package com.example.drawer2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class KauppaFragment : Fragment(R.layout.fragment_kauppa) {

    private var containerShoppingList: LinearLayout? = null
    private var etShoppingItem: EditText? = null
    private val PREFS_NAME = "shopping_prefs"
    private val KEY_ITEMS = "shopping_items"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        containerShoppingList = view.findViewById(R.id.containerShoppingList)
        etShoppingItem = view.findViewById(R.id.etShoppingItem)
        val btnAdd = view.findViewById<Button>(R.id.btnAddItem)

        loadItems()

        btnAdd.setOnClickListener {
            val itemName = etShoppingItem?.text?.toString()?.trim() ?: ""
            if (itemName.isNotEmpty()) {
                addItemToList(itemName)
                etShoppingItem?.text?.clear()
                saveItems()
            } else {
                context?.let { Toast.makeText(it, "Kirjoita ostoksen nimi!", Toast.LENGTH_SHORT).show() }
            }
        }
    }
    //ostoksen lisäys
    private fun addItemToList(name: String) {
        val context = context ?: return
        val inflater = LayoutInflater.from(context)
        val container = containerShoppingList ?: return
        
        val itemView = inflater.inflate(R.layout.item_shopping, container, false)
        val tvName = itemView.findViewById<TextView>(R.id.tvItemName)
        val btnDelete = itemView.findViewById<ImageView>(R.id.btnDelete)

        tvName.text = name
        btnDelete.setOnClickListener {
            container.removeView(itemView)
            saveItems()
        }
        container.addView(itemView)
    }
    //ostosten tallennus
    private fun saveItems() {
        val container = containerShoppingList ?: return
        val items = mutableListOf<String>()
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            val tvName = view.findViewById<TextView>(R.id.tvItemName)
            items.add(tvName.text.toString())
        }
        
        activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            putStringSet(KEY_ITEMS, items.toSet())
            apply()
        }
    }

    private fun loadItems() {
        val items = activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.getStringSet(KEY_ITEMS, emptySet())
        items?.forEach { addItemToList(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        containerShoppingList = null
        etShoppingItem = null
    }
}