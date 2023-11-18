package edu.puj.taller3

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Switch
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
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mMap: GoogleMap? = null
    private var swDisp: Switch? = null
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var data: Usuario? = null
    private var mDatabase: FirebaseDatabase? = null
    private var mRef: DatabaseReference? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private var locations: MutableList<Ubicacion>? = null
    private val justificacion = "Se necesita el GPS para mostrar la ubicación del evento"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        supportActionBar!!.title = "Home"
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(Color.parseColor("#287233")))
        swDisp = findViewById(R.id.sw)
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        locations = ArrayList()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        if (ContextCompat.checkSelfPermission(
                this@HomeActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission()
        }
        updateCurrentPosition()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()
        createNotificationChannel()
        val intent = Intent(this@HomeActivity, NotificationFirebaseIntentService::class.java)
        NotificationFirebaseIntentService.Companion.enqueueWork(this@HomeActivity, intent)
    }

    override fun onStart() {
        super.onStart()
        user = mAuth!!.currentUser
        initCurrentUser(user)
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
            initLocations()
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
                        this@HomeActivity,
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
                this@HomeActivity,
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
                if (location != null && data != null) {
                    swDisp!!.isChecked = data!!.disponible
                    val myLocation = LatLng(
                        data!!.latitude, data!!.longitude
                    )
                    if (mMap != null) {
                        mMap!!.clear()
                        initLocations()
                        mMap!!.addMarker(
                            MarkerOptions().position(myLocation).title("Current location")
                                .snippet("My Home").alpha(0.75f)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
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
                            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14f))
                        }
                    }
                }
            }
        }
    }

    private fun initLocations() {
        try {
            val json = JSONObject(loadJSONFromAsset())
            val loc = json.getJSONArray("locations")
            for (i in 0 until loc.length()) {
                val jsonO = loc.getJSONObject(i)
                val newLoc = Ubicacion(
                    jsonO.getString("name"),
                    jsonO.getString("latitude").toDouble(),
                    jsonO.getString("longitude").toDouble()
                )
                val ub = LatLng(newLoc.latitude, newLoc.longitude)
                mMap!!.addMarker(
                    MarkerOptions().position(ub).title(newLoc.name).alpha(0.75f)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                )
                locations!!.add(newLoc)
            }
        } catch (ex: JSONException) {
            ex.printStackTrace()
        }
    }

    fun loadJSONFromAsset(): String? {
        var json: String? = null
        json = try {
            val `is` = this.assets.open("locations.json")
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            kotlin.String(buffer, "UTF-8")
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val disp = menu.findItem(R.id.menuDisp)
        swDisp = disp.actionView as Switch?
        swDisp!!.setOnClickListener {
            if (swDisp!!.isChecked) {
                mRef = mDatabase!!.getReference(PATH_USERS + user!!.uid + "/" + "disponible")
                mRef!!.setValue(true)
            } else {
                mRef = mDatabase!!.getReference(PATH_USERS + user!!.uid + "/" + "disponible")
                mRef!!.setValue(false)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menuLogOut) {
            mAuth!!.signOut()
            val intent = Intent(this@HomeActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        } else if (id == R.id.menuLista) {
            val intent = Intent(this@HomeActivity, DisponiblesActivity::class.java)
            startActivity(intent)
        }
        return true
    }

    fun initCurrentUser(user: FirebaseUser?) {
        if (user != null) {
            mRef = mDatabase!!.getReference(PATH_USERS + user.uid)
            mRef!!.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    data = dataSnapshot.getValue(Usuario::class.java)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@HomeActivity, "Data retriving failed!", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "channel"
            val description = "channel description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val PATH_USERS = "users/"
        private const val REQUEST_CHECK_SETTINGS = 99
        private const val LOCATION_PERMISSION_CODE = 101
        private const val CHANNEL_ID = "MyApp"
        private const val TAG = "HomeActivity"
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