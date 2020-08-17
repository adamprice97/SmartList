package com.adam.shoppinglist

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.SparseBooleanArray
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.IOException
import java.io.InputStream

private var itemlist = arrayListOf<String>()
private var spiceList = arrayListOf<String>()
private lateinit var adapter: ArrayAdapter<String>

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            startForResult.launch(Intent(this, CaptureImage::class.java))
        }

        adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_multiple_choice
            , itemlist
        )

        listView.adapter = adapter

        loadData()
    }

    val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val UriString = result.data!!.getStringExtra("ImageUri")
                //process words
                val image: InputImage
                try {
                    image = InputImage.fromFilePath(this, Uri.parse(UriString))
                    val recognizer = TextRecognition.getClient()
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            // Task completed successfully
                            for (block in visionText.textBlocks) {
                                Log.w("AfterPic", block.text)
                                //val blockCornerPoints = block.cornerPoints
                                //val blockFrame = block.boundingBox
                                for (line in block.lines) {
                                    itemlist.add(line.text)
                                    listView.adapter = adapter
                                    adapter.notifyDataSetChanged()
                                    //val lineCornerPoints = line.cornerPoints
                                    //val lineFrame = line.boundingBox
                                    //for (element in line.elements) {
                                    //    val elementText = element.text
                                    //    val elementCornerPoints = element.cornerPoints
                                    //    val elementFrame = element.boundingBox
                                    //}
                                }
                            }
                            saveData()
                        }
                        .addOnFailureListener { e ->
                            // Task failed with an exception
                        }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId

        if (id == R.id.take_picture) {
            startForResult.launch(Intent(this, CaptureImage::class.java))
        }

        if (id == R.id.add_item) {
            val alert: AlertDialog.Builder =  AlertDialog.Builder(this)
            val edittext = EditText(this)
            alert.setTitle("Add Item To List")

            alert.setView(edittext)

            alert.setPositiveButton("Add",
                DialogInterface.OnClickListener { dialog, whichButton -> //What ever you want to do with the value
                    itemlist.add(edittext.text.toString())
                    adapter.notifyDataSetChanged()
                    saveData()
                })

            alert.setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, whichButton ->
                })

            alert.show()
            return true
        }
        if (id == R.id.clear_all) {
            itemlist.clear()
            adapter.notifyDataSetChanged()
            saveData()
            return true
        }

        if (id == R.id.clear_selected) {
            val position: SparseBooleanArray = listView.checkedItemPositions
            val count = listView.count
            var item = count - 1
            while (item >= 0) {
                if (position.get(item)) {
                    adapter.remove(itemlist.get(item))
                }
                item--
            }
            position.clear()
            adapter.notifyDataSetChanged()
            saveData()
            return true
        }

        return super.onOptionsItemSelected(item)

    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(itemlist)
        Log.w("Saving", json)
        editor.putString("ShoppingList", json)
        editor.apply()
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val gson = Gson()
        val emptyList = Gson().toJson(ArrayList<String>())
        val json = sharedPreferences.getString("ShoppingList", emptyList)
        val type = object : TypeToken<ArrayList<String>>() {}.type

        Log.w("loading", json!!)

        if (json != null) {
            itemlist.addAll(gson.fromJson(json, type))
            adapter.notifyDataSetChanged()
        }
    }
}


