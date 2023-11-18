package edu.puj.taller3

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import edu.puj.taller3.Model.Usuario
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
private lateinit var btnGallery: Button;
private lateinit var btnCamera: Button;
private lateinit var btnRegister: Button;
class RegistroActivity : AppCompatActivity() {
    private var etName: EditText? = null
    private var etApellido: EditText? = null
    private var etCorreo: EditText? = null
    private var etPassword: EditText? = null
    private var etIdentificacion: EditText? = null
    private var lat = -1.0
    private var lon = -1.0

    private var ivPhoto: ImageView? = null
    private var bmImage: Bitmap? = null
    private var mAuth: FirebaseAuth? = null
    private var mDatabase: FirebaseDatabase? = null
    private var mRef: DatabaseReference? = null //firabase realtime
    private var mStorage: StorageReference? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private val justificacion = "Se necesita el GPS para mostrar la ubicación del evento"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        supportActionBar!!.title = "Register"
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(Color.parseColor("#287233")))
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        mRef = mDatabase!!.getReference(PATH_USERS)
        mStorage = FirebaseStorage.getInstance().reference
        etName = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        etIdentificacion = findViewById(R.id.etIdentificacion)
        btnGallery = findViewById(R.id.btnGallery)
        btnCamera = findViewById(R.id.btnCamera)
        btnRegister = findViewById(R.id.btnRegister)
        ivPhoto = findViewById(R.id.ivPhoto)
        btnCamera.setOnClickListener(View.OnClickListener {
            accessCamera = requestPermission(
                this@RegistroActivity,
                Manifest.permission.CAMERA,
                "Permission to Access Camera",
                CAMERA
            )
            if (accessCamera) {
                usePermissionCamera()
            }
        })
        btnGallery.setOnClickListener(View.OnClickListener {
            accessAlm = requestPermission(
                this@RegistroActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                "Permission to Access Photo Gallery",
                ALMACENAMIENTO_EXTERNO
            )
            if (accessAlm) {
                usePermissionImage()
            }
        })
        btnRegister.setOnClickListener(View.OnClickListener { signUpUser() })
        if (ContextCompat.checkSelfPermission(
                this@RegistroActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocation()
        }
        updateCurrentPosition()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()
    }

    private fun requestLocation() {
        request(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
            justificacion,
            LOCATION_PERMISSION_CODE
        )
        if (ContextCompat.checkSelfPermission(
                this@RegistroActivity,
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
                    lat = location.latitude
                    lon = location.longitude
                }
            }
        }
    }

    private fun requestPermission(
        context: Activity,
        permit: String,
        justification: String,
        id: Int
    ): Boolean {
        return if (ContextCompat.checkSelfPermission(
                context,
                permit
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permit)) {
                Toast.makeText(this, justification, Toast.LENGTH_SHORT).show()
            }
            ActivityCompat.requestPermissions(context, arrayOf(permit), id)
            false
        } else {
            true
        }
    }

    private fun usePermissionCamera() {
        val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (pictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(pictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun usePermissionImage() {
        val pictureIntent = Intent(Intent.ACTION_PICK)
        pictureIntent.type = "image/*"
        startActivityForResult(pictureIntent, IMAGE_PICKER_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    usePermissionCamera()
                } else {
                    Toast.makeText(applicationContext, "Access denied to camera", Toast.LENGTH_LONG)
                        .show()
                }
            }

            ALMACENAMIENTO_EXTERNO -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    usePermissionImage()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Access denied to image gallery",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                if (resultCode == RESULT_OK) {
                    val extras = data!!.extras
                    val imageBitmap = extras!!["data"] as Bitmap?
                    bmImage = imageBitmap
                    ivPhoto!!.setImageBitmap(imageBitmap)
                }
            }

            IMAGE_PICKER_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    try {
                        val imageUri = data!!.data
                        val `is` = contentResolver.openInputStream(imageUri!!)
                        val selected = BitmapFactory.decodeStream(`is`)
                        bmImage = selected
                        ivPhoto!!.setImageBitmap(selected)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }

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

    override fun onStart() {
        super.onStart()
        val user = mAuth!!.currentUser
        updateUI(user)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val name = etName!!.text.toString()
            val apellido = etApellido!!.text.toString()
            val id = etIdentificacion!!.text.toString()
            if ((!name.equals("", ignoreCase = true)
                        && !apellido.equals("", ignoreCase = true) && !id.equals(
                    "",
                    ignoreCase = true
                ) && lat != -1.0) && lon != -1.0
            ) {
                //nuevo usuario
                val newUser = Usuario()
                newUser.name = name
                newUser.apellido = apellido
                newUser.id = id.toInt()
                newUser.latitude = lat
                newUser.longitude = lon
                newUser.disponible = true
                //path para cada usuario
                mRef = mDatabase!!.getReference(PATH_USERS + user.uid)
                mRef!!.setValue(newUser)
                val imgRef = mStorage!!.child(PATH_IMAGE + user.uid + "/" + "profile.png")
                imgRef.putBytes(uploadImage()).addOnSuccessListener {
                    Toast.makeText(this@RegistroActivity, "Welcome!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegistroActivity, HomeActivity::class.java)
                    startActivity(intent)
                }.addOnFailureListener {
                    Toast.makeText(
                        this@RegistroActivity,
                        "Registration failed!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            etCorreo!!.setText("")
            etPassword!!.setText("")
        }
    }

    private fun uploadImage(): ByteArray {
        val baos = ByteArrayOutputStream()
        bmImage!!.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }

    private fun signUpUser() {
        val user = etCorreo!!.text.toString()
        val password = etPassword!!.text.toString()
        if (!user.isEmpty() && !password.isEmpty()) {
            mAuth!!.createUserWithEmailAndPassword(user, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = mAuth!!.currentUser
                        updateUI(user)
                    } else {
                        Toast.makeText(
                            this@RegistroActivity,
                            "Registration failed: " + task.exception!!.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
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

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val IMAGE_PICKER_REQUEST = 4
        private const val CAMERA = 2
        private const val ALMACENAMIENTO_EXTERNO = 3
        private var accessCamera = false
        private var accessAlm = false
        private const val PATH_USERS = "users/"
        private const val PATH_IMAGE = "images/"
        private const val REQUEST_CHECK_SETTINGS = 99
        private const val LOCATION_PERMISSION_CODE = 101

        //permisos
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