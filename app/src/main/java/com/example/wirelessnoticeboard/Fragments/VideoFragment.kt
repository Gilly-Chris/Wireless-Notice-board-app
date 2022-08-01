package com.example.wirelessnoticeboard.Fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.wirelessnoticeboard.Util
import com.example.wirelessnoticeboard.databinding.VideoFragmentBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class VideoFragment : Fragment() {
    private var _binding : VideoFragmentBinding? = null
    private lateinit var database: DatabaseReference
    private lateinit var progressDialog : ProgressDialog
    private lateinit var storageRef : StorageReference
    private var videoUri : Uri? = null

    private val binding get() = _binding!!

    companion object {
        fun newInstance(): VideoFragment {
            return VideoFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflater?.let { VideoFragmentBinding.inflate(it, container, false) }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if(!checkPersmission()){
            requestPermission()
        }
        database = FirebaseDatabase.getInstance().reference
        progressDialog = ProgressDialog(context)

        binding.uploadVideoBtn.setOnClickListener {
            chooseVideo()
        }

        binding.sendVideoBtn.setOnClickListener {
            if (videoUri != null) {
                // save the selected video in Firebase storage
                uploadVideo()
            }else {
                Toast.makeText(requireContext(), "No video selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun chooseVideo() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, 5)
    }

    private fun getFileType(videoUri: Uri): String? {
        val r: ContentResolver = requireContext().contentResolver
        // get the file type ,in this case its mp4
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(r.getType(videoUri))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 5 && resultCode == RESULT_OK && data != null && data.data != null) {
            videoUri = data.data;
            Glide.with(requireContext()).load(videoUri).into(binding.videoThumb)
        }
    }

    private fun checkPersmission(): Boolean {
        return (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ),
            ImageFragment.PERMISSION_REQUEST_CODE
        )
    }

    private fun uploadVideo() {
        progressDialog.setTitle("Video upload")
        progressDialog.setMessage("Sending video...")
        progressDialog.show()
        storageRef = FirebaseStorage.getInstance()
            .getReference("videos/" + System.currentTimeMillis() + "_video" + "." + getFileType(videoUri!!))
        storageRef.putFile(videoUri!!).addOnSuccessListener { taskSnapshot ->
            val uriTask = taskSnapshot.storage.downloadUrl
            while (!uriTask.isSuccessful);
            // get the link of video
            val downloadUri = uriTask.result.toString()
            updateMessage(downloadUri = downloadUri)
            progressDialog.dismiss()
        }.addOnFailureListener{
            progressDialog.dismiss()
            Toast.makeText(context, "There was an error, please try again", Toast.LENGTH_LONG).show()
            it.message?.let { it1 -> Log.w("VideoFragment", it1) }
        }.addOnProgressListener {
            val progress: Double =
                100.0 * it.bytesTransferred / it.totalByteCount
            progressDialog.setMessage("Uploaded " + progress.toInt() + "%")
        }
    }

    private fun updateMessage(downloadUri: String){
        val type = Util.Type.VIDEO
        val priority = if(binding.priority.isChecked) Util.Priority.IMPORTANT else Util.Priority.CASUAL
        val data = Message(video = downloadUri, type = type.toString(), priority = priority.toString())
        database.child("data").setValue(data).addOnSuccessListener {
            Toast.makeText(context, "Video uploaded successfully!!", Toast.LENGTH_LONG).show()
        }.addOnFailureListener {
            Toast.makeText(context, "There was an error, please try again", Toast.LENGTH_LONG).show()
            it.message?.let { it1 -> Log.w("VideoFragment", it1) }
        }
    }

    data class Message(val video: String, val type: String, val priority: String)
}