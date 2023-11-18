package edu.puj.taller3

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import edu.puj.taller3.Model.Usuario
import java.io.File
import java.io.IOException

class DisponiblesAdapter(
    private val context: Context,
    private val usuarios: List<Usuario?>,
    private val currUserId: String
) : ArrayAdapter<Usuario?>(
    context, R.layout.disponibles, usuarios
) {
    private val mStorage: StorageReference

    init {
        mStorage = FirebaseStorage.getInstance().reference
    }

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val mView = LayoutInflater.from(context).inflate(R.layout.disponibles, viewGroup, false)
        val name = mView.findViewById<TextView>(R.id.tvName)
        val apellido = mView.findViewById<TextView>(R.id.tvLast)
        val btnLoc = mView.findViewById<Button>(R.id.btnLocation)
        if (!currUserId.equals(usuarios[i]!!.key, ignoreCase = true)) {
            btnLoc.setOnClickListener { view ->
                val intent = Intent(getContext(), MapsActivity::class.java)
                intent.putExtra("key", usuarios[i]!!.key)
                view.context.startActivity(intent)
            }
        } else {
            btnLoc.text = "Estoy Conectado!"
            btnLoc.isClickable = false
        }
        val ivPhoto = mView.findViewById<ImageView>(R.id.ivProfile)
        try {
            downloadFile(usuarios[i]!!.key, ivPhoto)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        name.text = usuarios[i]!!.name
        apellido.text = usuarios[i]!!.apellido
        return mView
    }

    @Throws(IOException::class)
    private fun downloadFile(id: String, ivPhoto: ImageView) {
        val localFile = File.createTempFile("images", "png")
        val imageRef = mStorage.child(PATH_IMAGE + id + "/profile.png")
        imageRef.getFile(localFile)
            .addOnSuccessListener { ivPhoto.setImageURI(Uri.fromFile(localFile)) }
            .addOnFailureListener { }
    }

    companion object {
        private const val PATH_IMAGE = "images/"
    }
}