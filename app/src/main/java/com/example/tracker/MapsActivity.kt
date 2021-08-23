package com.example.tracker


import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.tracker.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase



class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private val LOCATION_PERMISSION_REQUEST = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    lateinit var databaseRef: DatabaseReference
    lateinit var floatingActionButton: FloatingActionButton

    //to access mile location
    //whether the device access permission is allowed by the user and asks again if not granted



    private fun getLocationAccess(){
        if(ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED){
            //map.isMyLocationEnabled = true
            getLocationUpdates()
            startLocationUpdates()
        }else {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST)
        }
    }


    //opens the map if allowed, toast a message otherwise. works with the getLocationAccess()
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST){
            if(grantResults.contains(PackageManager.PERMISSION_GRANTED)){
                getLocationAccess()
            }else{
                Toast.makeText(this, "Permission needs to be granted for this app to be functional", Toast.LENGTH_LONG).show()
//                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //
        fusedLocationClient =  LocationServices.getFusedLocationProviderClient(this)
    }

    //fetching location from firebase on location path
    val logListener = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(applicationContext, "Could not read from database", Toast.LENGTH_SHORT).show()
        }

        //using datasnapshot to get the data from firebase
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.exists()) {

                val locationlogging = dataSnapshot.child("location").getValue(LocationLogging::class.java)
                val driverLat=locationlogging?.Latitude
                val driverLong=locationlogging?.Longitude

                if (driverLat !=null  && driverLong != null) {
                    val driverLoc = LatLng(driverLat, driverLong)

                    val markerOptions = MarkerOptions().position(driverLoc).title("Rasheed")
                        //.icon(BitmapDescriptorFactory.fromResource(R.drawable.indicator))
                    map.addMarker(markerOptions)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(driverLoc, 15f))
                    //Zoom level - 1: World, 5: Landmass/continent, 10: City, 15: Streets and 20: Buildings

                    Toast.makeText(applicationContext, "Locations accessed from the database", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    //to get the location every 5 seconds
    private fun getLocationUpdates() {
        locationRequest = LocationRequest()
        locationRequest.interval = 8_000
        locationRequest.fastestInterval = 5_000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.lastLocation

                    //passing the co-ordinates to firebase
                    databaseRef = Firebase.database.reference

                    val locationlogging = LocationLogging(location.latitude, location.longitude)
                    databaseRef.addValueEventListener(logListener)
                    databaseRef.child("Tracker").setValue(locationlogging)
                        .addOnSuccessListener {
                            Toast.makeText(applicationContext, "Locations written into the database", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(applicationContext, "Error occured while writing the locations", Toast.LENGTH_LONG).show()
                        }


                    val latLng = LatLng(location.latitude, location.longitude)
                    val markerOptions = MarkerOptions().position(latLng)
                    map.addMarker(markerOptions)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }
    }





    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }




    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    //the function called when the map loads on the screen
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        floatingActionButton = findViewById(R.id.floatingActionButton)

        floatingActionButton.setOnClickListener {
            getLocationAccess()
        }


        //to get the extent of the zoom. 10f to city, 15f to street and 20f to building level.
//        val zoomLevel = 20f
////        // to get the map to show location in benin
//        val benin = LatLng(6.339185, 5.617447)
//        map.addMarker(MarkerOptions().position(benin).title("Benin, Nigeria").icon(BitmapDescriptorFactory
//            .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
//        //to change the map indicator
//        //.fromResource(R.drawable.item)
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(benin, zoomLevel))
    }
}