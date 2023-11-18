package edu.puj.taller3

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import edu.puj.taller3.Model.Usuario

class DisponiblesActivity : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var mDatabase: FirebaseDatabase? = null
    private var mRef: DatabaseReference? = null
    private var data: Usuario? = null
    private var adapter: DisponiblesAdapter? = null
    private var listView: ListView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disponibles)
        supportActionBar!!.title = "Disponibles"
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(Color.parseColor("#287233")))
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        mRef = mDatabase!!.getReference(PATH_USERS)
        listView = findViewById(R.id.lvLayout)
    }

    override fun onStart() {
        super.onStart()
        user = mAuth!!.currentUser
        initCurrentUser(user)
    }

    fun initCurrentUser(user: FirebaseUser?) {
        if (user != null) {
            mRef = mDatabase!!.getReference(PATH_USERS + user.uid)
            mRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    data = dataSnapshot.getValue(Usuario::class.java)
                    data!!.key = user.uid
                    initDisponibles()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(
                        this@DisponiblesActivity,
                        "Data recollection failed!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    private fun initDisponibles() {
        mRef = mDatabase!!.getReference(PATH_USERS)
        mRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val disponibles: MutableList<Usuario?> = ArrayList()
                for (entity in dataSnapshot.children) {
                    val usuario = entity.getValue(Usuario::class.java)
                    if (usuario!!.disponible) {
                        usuario.key = entity.key
                        disponibles.add(usuario)
                    }
                }
                adapter = DisponiblesAdapter(this@DisponiblesActivity, disponibles, data!!.key)
                listView!!.adapter = adapter
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    companion object {
        private const val PATH_USERS = "users/"
    }
}