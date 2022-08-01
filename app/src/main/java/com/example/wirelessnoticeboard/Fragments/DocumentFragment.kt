package com.example.wirelessnoticeboard.Fragments

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.wirelessnoticeboard.Util
import com.example.wirelessnoticeboard.databinding.DocumentFragmentBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*


class DocumentFragment : Fragment() {

    private var _binding : DocumentFragmentBinding? = null
    private lateinit var database: DatabaseReference
    private lateinit var storageRef: StorageReference
    private lateinit var progressDialog : ProgressDialog
    private var document : Uri? = null

    private val binding get() = _binding!!

    companion object {
        fun newInstance() : DocumentFragment {
            return DocumentFragment()
        }

        const val DOCUMENT_REQUEST_CODE = 1
        val Fragment.packageManager get() = activity?.packageManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflater?.let { DocumentFragmentBinding.inflate(it, container, false) }
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        database = FirebaseDatabase.getInstance().reference

        progressDialog = ProgressDialog(context)

        binding.uploadDocsBtn.setOnClickListener {
            val galleryIntent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT
            galleryIntent.type = "application/pdf"
            startActivityForResult(galleryIntent, DOCUMENT_REQUEST_CODE)
        }

        binding.sendDocsBtn.setOnClickListener {
            if (!Util.isNetworkAvailable(context)) {
                Toast.makeText(context, "No Internet Connection!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if(document == null){
                Toast.makeText(context, "No document selected!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            progressDialog.setTitle("Display Document")
            progressDialog.setMessage("Uploading Document, please wait...")
            progressDialog.show()

            storageRef = FirebaseStorage.getInstance().reference.child("documents/" + UUID.randomUUID().toString() + "_docs.pdf")
            storageRef.putFile(document!!).addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener { uri ->
                    updateMessage(downloadUri = uri)
                }
                progressDialog.dismiss()
            }.addOnFailureListener{
                it.message?.let { it1 -> Log.w("DocumentFragment", it1) }
                progressDialog.dismiss()
            }.addOnProgressListener {
                val progress: Double =
                    100.0 * it.bytesTransferred / it.totalByteCount
                progressDialog.setMessage("Uploaded " + progress.toInt() + "%")
            }
        }
    }

    private fun updateMessage(downloadUri : Uri){
        val type = Util.Type.DOCUMENT
        val priority = if(binding.priority.isChecked) Util.Priority.IMPORTANT else Util.Priority.CASUAL
        val delay = if(binding.delay.isChecked) "TRUE" else "FALSE"
        val data = DocumentFragment.Message(
            document = downloadUri.toString(),
            type = type.toString(),
            priority = priority.toString(),
            delay = delay
        )
        database.child("data").setValue(data).addOnSuccessListener {
            Toast.makeText(context, "Document uploaded successfully!!", Toast.LENGTH_LONG).show()
        }.addOnFailureListener {
            Toast.makeText(context, "There was an error, please try again", Toast.LENGTH_LONG).show()
            it.message?.let { it1 -> Log.w("DocumentFragment", it1) }
        }
    }

    @SuppressLint("Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode === RESULT_OK) {
            if (data != null) {
                val selectedDoc = data.data!!
                document = selectedDoc
                if (selectedDoc.toString().startsWith("content://")) {
                    var myCursor: Cursor? = null
                    try {
                        // Setting the PDF to the TextView
                        myCursor = requireContext().contentResolver.query(selectedDoc, null, null, null, null)
                        if (myCursor != null && myCursor.moveToFirst()) {
                            var pdfName : String = myCursor.getString(myCursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME))
                            binding.documentName.text = pdfName
                        }
                    } finally {
                        myCursor?.close()
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(), "Cancelled",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
    }

    data class Message(val document: String, val type: String, val priority: String, val delay: String)
}