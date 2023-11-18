package edu.puj.taller3

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import edu.puj.taller3.Model.Usuario

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mMap: GoogleMap? = null
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var data: Usuario? = null
    private var other: Usuario? = null
    private var mDatabase: FirebaseDatabase? = null
    private var mRef: DatabaseReference? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private val justificacion = "Se necesita el GPS para mostrar la ubicación del evento"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        supportActionBar!!.title = "Seguimiento"
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(Color.parseColor("#287233")))
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.followMap) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        user = mAuth!!.currentUser
        if (user == null) {
            val intent = Intent(this@MapsActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        initCurrentUser(user)
        if (ContextCompat.checkSelfPermission(
                this@MapsActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission()
        }
        updateCurrentPosition()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.uiSettings.isZoomGesturesEnabled = true
        mMap!!.uiSettings.isZoomControlsEnabled = true
        mMap!!.uiSettings.isMyLocationButtonEnabled = true
        mMap!!.uiSettings.isCompassEnabled = true
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission()
        } else {
            mMap!!.isMyLocationEnabled = true
            try {
                MapsInitializer.initialize(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            updateCurrentPosition()
        }
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(4.65, -74.05), 12f))
        val builder = LocationSettingsRequest.Builder().addLocationRequest(
            mLocationRequest!!
        )
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnFailureListener(this) { e ->
            val statusCode = (e as ApiException).statusCode
            when (statusCode) {
                CommonStatusCodes.RESOLUTION_REQUIRED -> try {
                    val resolvableApiException = e as ResolvableApiException
                    resolvableApiException.startResolutionForResult(
                        this@MapsActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (ex: SendIntentException) {
                    ex.printStackTrace()
                }

                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {}
            }
        }
    }

    private fun requestPermission() {
        request(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
            justificacion,
            LOCATION_PERMISSION_CODE
        )
        if (ContextCompat.checkSelfPermission(
                this@MapsActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val builder = LocationSettingsRequest.Builder().addLocationRequest(
                mLocationRequest!!
            )
            val client = LocationServices.getSettingsClient(this)
            val task = client.checkLocationSettings(builder.build())
            task.addOnSuccessListener(this) { startLocationUpdates() }
        }
    }

    protected fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority =
            LocationRequest.PRIORITY_HIGH_ACCURACY
        return locationRequest
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient!!.requestLocationUpdates(
                mLocationRequest!!,
                mLocationCallback!!,
                null
            )
        }
    }

    fun updateCurrentPosition() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    val myLocation = LatLng(
                        data!!.latitude, data!!.longitude
                    )
                    val otherLocation = LatLng(
                        other!!.latitude, other!!.longitude
                    )
                    if (mMap != null) {
                        mMap!!.clear()
                        mMap!!.addMarker(
                            MarkerOptions().position(myLocation).title("Current location")
                                .alpha(0.75f)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )
                        mMap!!.addMarker(
                            MarkerOptions().position(otherLocation).title("Other user location")
                                .alpha(0.75f)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        )
                        if (location.latitude != data!!.latitude && location.longitude != data!!.longitude) {
                            data!!.latitude = location.latitude
                            data!!.longitude = location.longitude
                            mRef =
                                mDatabase!!.getReference(PATH_USERS + user!!.uid + "/" + "latitude")
                            mRef!!.setValue(location.latitude)
                            mRef =
                                mDatabase!!.getReference(PATH_USERS + user!!.uid + "/" + "longitude")
                            mRef!!.setValue(location.longitude)
                            val dist = distance(
                                data!!.latitude,
                                data!!.longitude,
                                other!!.latitude,
                                other!!.longitude
                            )
                            Toast.makeText(
                                this@MapsActivity,
                                "Distance is: $dist km",
                                Toast.LENGTH_LONG
                            ).show()
                            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14f))
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == RESULT_OK) {
                    startLocationUpdates()
                } else {
                    Toast.makeText(
                        this,
                        "Sin acceso a localización, hardware deshabilitado!", Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
        }
    }

    fun initCurrentUser(user: FirebaseUser?) {
        if (user != null) {
            mRef = mDatabase!!.getReference(PATH_USERS + user.uid)
            mRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    data = dataSnapshot.getValue(Usuario::class.java)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@MapsActivity, "Data retriving failed!", Toast.LENGTH_SHORT)
                        .show()
                }
            })
            mRef = mDatabase!!.getReference(PATH_USERS + intent.getStringExtra("key"))
            mRef!!.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    other = dataSnapshot.getValue(Usuario::class.java)
                    val dist = distance(
                        data!!.latitude,
                        data!!.longitude,
                        other!!.latitude,
                        other!!.longitude
                    )
                    Toast.makeText(this@MapsActivity, "Distance is: $dist km", Toast.LENGTH_LONG)
                        .show()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@MapsActivity, "Data retriving failed!", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }
    }

    private fun distance(
        myLat: Double,
        myLong: Double,
        otherLat: Double,
        otherLong: Double
    ): Double {
        val latDistance = Math.toRadians(myLat - otherLat)
        val longDistance = Math.toRadians(myLong - otherLong)
        val a =
            Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(myLat)) *
                    Math.cos(Math.toRadians(otherLat)) * Math.sin(longDistance / 2) * Math.sin(
                longDistance / 2
            )
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val res = 6371.01 * c
        return Math.round(res * 100.0) / 100.0
    }

    public override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    public override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    public override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient!!.removeLocationUpdates(mLocationCallback!!)
    }

    companion object {
        private const val PATH_USERS = "users/"
        private const val REQUEST_CHECK_SETTINGS = 99
        private const val LOCATION_PERMISSION_CODE = 101
        fun request(
            activity: Activity?,
            permissionCode: String,
            justificacion: String?,
            idCode: Int
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity!!,
                    permissionCode
                )
            ) {
                AlertDialog.Builder(activity)
                    .setTitle("Se necesita un permiso")
                    .setMessage(justificacion)
                    .setPositiveButton("ok") { dialog, which ->
                        ActivityCompat.requestPermissions(
                            activity, arrayOf(permissionCode), idCode
                        )
                    }
                    .setNegativeButton("cancel") { dialog, which -> dialog.dismiss() }
                    .create().show()
            } else {
                ActivityCompat.requestPermissions(activity, arrayOf(permissionCode), idCode)
            }
        }
    }
}