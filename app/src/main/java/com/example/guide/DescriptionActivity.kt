package com.example.guide

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DescriptionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_LOCATION_NAME = "extra_location_name" // Add this line
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_description)

        val locationNameTextView = findViewById<TextView>(R.id.locationNameTextView)
        val descriptionTextView = findViewById<TextView>(R.id.descriptionTextView)


        val locationName = intent.getStringExtra(EXTRA_LOCATION_NAME)
        val description = intent.getStringExtra(EXTRA_DESCRIPTION)

        locationNameTextView.text = locationName
        descriptionTextView.text = description

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }



    }
}
