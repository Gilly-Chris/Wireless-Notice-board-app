package com.example.wirelessnoticeboard

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wirelessnoticeboard.databinding.LoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding : LoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog : ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        auth = Firebase.auth
        progressDialog = ProgressDialog(this)

        val emailField = binding.emailField
        val passwordField = binding.passwordField

        binding.loginBtn.setOnClickListener {
            if(!Util.isNetworkAvailable(this)){
                Toast.makeText(this, "No Internet Connection!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            var email = emailField.text.toString()
            var password = passwordField.text.toString()

            Log.d("Email Value", email);

            if(TextUtils.isEmpty(email)){
                emailField.requestFocus()
                Toast.makeText(this, "Please fill the form properly", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(TextUtils.isEmpty(password)){
                passwordField.requestFocus()
                Toast.makeText(this, "Please fill the form properly", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            signInUser(email, password)
        }
    }

    private fun signInUser(email: String, password: String){
        progressDialog.setTitle("Sign in")
        progressDialog.setMessage("Signing in, please wait...")
        progressDialog.show()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("user", user)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("user", currentUser)
            }
            startActivity(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        if(progressDialog != null){
            progressDialog.dismiss()
        }
    }

    companion object {
        const val TAG = "LoginActivity"
    }
}