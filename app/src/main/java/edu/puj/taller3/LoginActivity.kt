package edu.puj.taller3

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import edu.puj.taller3.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null
    var binding: ActivityLoginBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        supportActionBar!!.title = "Main Page"
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(Color.parseColor("#287233")))
        mAuth = FirebaseAuth.getInstance()
        binding!!.btnLogin.setOnClickListener { attemptSignIn() }
        binding!!.btnRegister.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        val user = mAuth!!.currentUser
        updateUI(user)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        } else {
            binding!!.etCorreo.setText("")
            binding!!.etPassword.setText("")
        }
    }

    private fun attemptSignIn() {
        val user = binding!!.etCorreo.text.toString()
        val password = binding!!.etPassword.text.toString()
        if (!user.equals("", ignoreCase = true) && !password.equals("", ignoreCase = true)) {
            mAuth!!.signInWithEmailAndPassword(user, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = mAuth!!.currentUser
                        Toast.makeText(this@LoginActivity, "Bienvenido", Toast.LENGTH_SHORT).show()
                        updateUI(user)
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Autenticación fallida",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI(null)
                    }
                }
        } else {
            Toast.makeText(this, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show()
        }
    }
}