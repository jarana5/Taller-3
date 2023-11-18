package edu.puj.taller3

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import edu.puj.taller3.Model.Usuario

class NotificationFirebaseIntentService : JobIntentService() {
    var firebaseDatabase: FirebaseDatabase? = null
    var firebaseAuth: FirebaseAuth? = null
    var myRef: DatabaseReference? = null
    private var user: FirebaseUser? = null
    private var data: Usuario? = null
    private val anteriores: MutableList<Usuario?> = ArrayList()
    override fun onCreate() {
        super.onCreate()
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        myRef = firebaseDatabase!!.getReference(PATH_USERS)
        user = firebaseAuth!!.currentUser
        initCurrentUser(user)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onHandleWork(intent: Intent) {
        myRef = firebaseDatabase!!.getReference(PATH_USERS)
        myRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (entity in dataSnapshot.children) {
                    val usuario = entity.getValue(Usuario::class.java)
                    usuario!!.key = entity.key
                    val changeAux = stateChange(usuario)
                    if (firebaseAuth!!.currentUser != null && usuario.disponible && usuario.key != data!!.key) {
                        buildAndShowNotification(
                            "Usuario Disponible",
                            "El usuario: " + usuario.name + " se encuentra DISPONIBLE",
                            usuario.key
                        )
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun llenarInicial() {
        myRef = firebaseDatabase!!.getReference(PATH_USERS)
        myRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (entity in dataSnapshot.children) {
                    val usuario = entity.getValue(Usuario::class.java)
                    usuario!!.key = entity.key
                    anteriores.add(usuario)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun initCurrentUser(user: FirebaseUser?) {
        if (user != null) {
            myRef = firebaseDatabase!!.getReference(PATH_USERS + user.uid)
            myRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    data = dataSnapshot.getValue(Usuario::class.java)
                    data!!.key = dataSnapshot.key
                    llenarInicial()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(
                        this@NotificationFirebaseIntentService,
                        "Data retriving failed!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    private fun stateChange(userN: Usuario?): Boolean {
        var change = false
        var pos = -1
        for (i in anteriores.indices) {
            if (anteriores[i]!!.key == userN!!.key) {
                if (!anteriores[i]!!.disponible && userN.disponible) {
                    change = true
                    pos = i
                    break
                }
            }
        }
        if (pos != -1) {
            anteriores.removeAt(pos)
            anteriores.add(userN)
        }
        return change
    }

    private fun buildAndShowNotification(title: String, message: String, userKey: String) {
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        mBuilder.setSmallIcon(R.drawable.img)
        mBuilder.setContentTitle(title)
        mBuilder.setContentText(message)
        mBuilder.priority = NotificationCompat.PRIORITY_DEFAULT
        val intent = Intent(this, MapsActivity::class.java)
        intent.putExtra("key", userKey)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        mBuilder.setContentIntent(pendingIntent)
        mBuilder.setAutoCancel(true)
        val notificationId = 1
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(notificationId, mBuilder.build())
    }

    companion object {
        private const val JOB_ID = 13
        private const val CHANNEL_ID = "MyApp"
        private const val PATH_USERS = "users/"
        fun enqueueWork(context: Context?, intent: Intent?) {
            enqueueWork(context!!, NotificationFirebaseIntentService::class.java, JOB_ID, intent!!)
        }
    }
}