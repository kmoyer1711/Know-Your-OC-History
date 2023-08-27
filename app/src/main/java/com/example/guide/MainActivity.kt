package com.example.guide

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory

class MainActivity : AppCompatActivity() {

    // Constants for geofence management
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "Geofence_Channel"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val GEOFENCE_ID = "Geofence_ID_"
    }

    private var geofencesCreated = false  // Flag to track if geofences have been created

    // Data class for geofence data
    data class GeofenceData(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val radius: Float,
        val description: String
    )

    // Function to read data from the spreadsheet and create a list of GeofenceData
    fun readFromSpreadsheet(context: Context): List<GeofenceData> {
        val geofenceDataList = mutableListOf<GeofenceData>()

        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("OC_locations.xls")
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            for (rowIndex in 0..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex)
                val name = row.getCell(2).stringCellValue
                val latLngCell = row.getCell(4)

                // Convert latitude and longitude values to double
                val latLngValue = if (latLngCell.cellType == CellType.STRING)
                    latLngCell.stringCellValue
                else
                    latLngCell.numericCellValue.toString()

                val (longitude, latitude) = latLngValue.split(",").map { it.trim().toDouble() }
                val radius = 2000f // Set default radius to 2000 meters (2 km)
                val description =
                    row.getCell(3).stringCellValue // Assuming description is stored as a string

                val geofenceData = GeofenceData(name, latitude, longitude, radius, description)
                geofenceDataList.add(geofenceData)
            }

            workbook.close()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return geofenceDataList
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        showMainMenu()

        if (!geofencesCreated) {
            requestLocationPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Location permission is granted.
            // You can proceed with location-related functionality.
            createGeofencesAndMonitor()
        } else {
            // Location permission is not granted.
            // Request the permission from the user.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION // Include background location permission
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission is granted.
                // You can proceed with location-related functionality.
                createGeofencesAndMonitor()
            } else {
                // Location permission is denied.
                // Handle the case when the user denies the permission.
                // For simplicity, let's just finish the activity when permission is denied.
                finish()
            }
        }
    }

    private fun showMainMenu() {
        // Now, show the rest of the main menu layout, e.g., buttons, views, etc.
        // Find your buttons, set click listeners, etc.

        val buttonAbout = findViewById<Button>(R.id.aboutbutton)
        buttonAbout.setOnClickListener {
            // Handle the "About the app" button click
            // Start a new activity to show the About page
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        val buttonViewLocations = findViewById<Button>(R.id.locationsbutton)
        buttonViewLocations.setOnClickListener {
            // Open the link when the "View All Locations" button is clicked
            val url =
                "https://www.google.com/maps/d/viewer?mid=1gtl8cLHxd1gqtSWhkED0TLZL4dX42xM&usp=sharing"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    private fun createGeofencesAndMonitor() {
        try {
            val spreadsheetData = readFromSpreadsheet(this)

            val geofenceList = mutableListOf<Geofence>()

            for (data in spreadsheetData) {
                val latitude = data.latitude
                val longitude = data.longitude
                val radius = data.radius

                val geofence: Geofence = Geofence.Builder()
                    .setRequestId(GEOFENCE_ID + data.name)
                    .setCircularRegion(
                        latitude,
                        longitude,
                        radius
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
                    ) // Use GEOFENCE_TRANSITION_DWELL for dwell events
                    .setLoiteringDelay(10000) // Set the dwell time threshold in milliseconds
                    .build()
                geofenceList.add(geofence)
            }

            val geofencingRequest: GeofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build()

            val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            } else {
                LocationServices.getGeofencingClient(this)
                    .addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener {
                        // Geofences added successfully
                    }
                    .addOnFailureListener {
                        // Handle failure to add geofences
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle the exception, log an error message, or take appropriate action
        }
    }

}