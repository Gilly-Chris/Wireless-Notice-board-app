package com.example.wirelessnoticeboard.Fragments

import android.app.ProgressDialog
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.wirelessnoticeboard.Util
import com.example.wirelessnoticeboard.databinding.TextFragmentBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class TextFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var progressDialog : ProgressDialog

    private var _binding : TextFragmentBinding? = null

    private val binding get() = _binding!!

    companion object {
        fun newInstance() : TextFragment {
            return TextFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflater?.let { TextFragmentBinding.inflate(it, container, false) }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        database = FirebaseDatabase.getInstance().reference
        progressDialog = ProgressDialog(context)

        binding.sendTextBtn.setOnClickListener {
            if(!Util.isNetworkAvailable(context)){
                Toast.makeText(context, "No Internet Connection!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            var message = binding.displayField.text
            if(TextUtils.isEmpty(message)){
                Toast.makeText(context, "Enter Message!", Toast.LENGTH_LONG).show()
                binding.displayField.requestFocus()
                return@setOnClickListener
            }

            val type = Util.Type.TEXT
            val priority = if(binding.priority.isChecked) Util.Priority.IMPORTANT else Util.Priority.CASUAL
            val delay = if(binding.delay.isChecked) "TRUE" else "FALSE"
            val data = Message(message = message.toString(), type = type.toString(), priority = priority.toString(), delay = delay)

            progressDialog.setTitle("Display Message")
            progressDialog.setMessage("Sending message, please wait...")
            database.child("data").setValue(data).addOnSuccessListener {
                progressDialog.dismiss()
                binding.displayField.setText("")
                Toast.makeText(context, "Uploaded successfully!!", Toast.LENGTH_LONG).show()
            }.addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(context, "There was an error, please try again", Toast.LENGTH_LONG).show()
                it.message?.let { it1 -> Log.w("TextFragment", it1) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class Message(val message: String, val type: String, val priority: String, val delay: String)
}