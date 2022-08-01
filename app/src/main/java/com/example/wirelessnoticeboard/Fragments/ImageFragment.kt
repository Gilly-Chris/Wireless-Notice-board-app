package com.example.wirelessnoticeboard.Fragments

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.wirelessnoticeboard.Util
import com.example.wirelessnoticeboard.databinding.ImageFragmentBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class ImageFragment : Fragment() {
    private var _binding : ImageFragmentBinding? = null
    private lateinit var database: DatabaseReference
    private lateinit var storageRef: StorageReference
    private var mCurrentPhotoPath : String? = null
    private lateinit var progressDialog : ProgressDialog
    private var image : Uri? = null

    private val binding get() = _binding!!

    companion object {
        fun newInstance() : ImageFragment {
            return ImageFragment()
        }
        const val GALLERY_PICTURE = 1
        const val CAMERA_REQUEST = 2
        const val PERMISSION_REQUEST_CODE = 3
        val Fragment.packageManager get() = activity?.packageManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflater?.let { ImageFragmentBinding.inflate(it, container, false) }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if(!checkPermission()){
            requestPermission()
        }
        database = FirebaseDatabase.getInstance().reference

        progressDialog = ProgressDialog(context)

        binding.uploadImageBtn.setOnClickListener {
            startDialog()
        }
        binding.sendImageBtn.setOnClickListener {
            if (!Util.isNetworkAvailable(context)) {
                Toast.makeText(context, "No Internet Connection!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if(image == null){
                Toast.makeText(context, "No image selected!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            progressDialog.setTitle("Display Image")
            progressDialog.setMessage("Uploading Image, please wait...")
            progressDialog.show()

            storageRef = FirebaseStorage.getInstance().reference.child("images/" + UUID.randomUUID().toString() + "_img.png")
            storageRef.putFile(image!!).addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener { uri ->
                    updateMessage(downloadUri = uri)
                }
                progressDialog.dismiss()
            }.addOnFailureListener{
                it.message?.let { it1 -> Log.w("ImageFragment", it1) }
                progressDialog.dismiss()
            }.addOnProgressListener {
                val progress: Double =
                    100.0 * it.bytesTransferred / it.totalByteCount
                progressDialog.setMessage("Uploaded " + progress.toInt() + "%")
            }
        }
    }

    private fun updateMessage(downloadUri : Uri){
        val type = Util.Type.IMAGE
        val priority = if(binding.priority.isChecked) Util.Priority.IMPORTANT else Util.Priority.CASUAL
        val delay = if(binding.delay.isChecked) "TRUE" else "FALSE"
        val data = Message(image = downloadUri.toString(), type = type.toString(), priority = priority.toString(), delay = delay)
        database.child("data").setValue(data).addOnSuccessListener {
            Toast.makeText(context, "Image uploaded successfully!!", Toast.LENGTH_LONG).show()
        }.addOnFailureListener {
            Toast.makeText(context, "There was an error, please try again", Toast.LENGTH_LONG).show()
            it.message?.let { it1 -> Log.w("ImageFragment", it1) }
        }
    }

    private fun checkPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(requireContext(), CAMERA) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireContext(),
            READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(READ_EXTERNAL_STORAGE, CAMERA),
            PERMISSION_REQUEST_CODE)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun startDialog() {
        val dialog : AlertDialog.Builder = AlertDialog.Builder(requireContext())
        dialog.setTitle("Upload Pictures Option")
        dialog.setMessage("How do you want to set your picture?")
        dialog.setPositiveButton(
            "Gallery"
        ) { _, _ ->
            var pictureActionIntent: Intent?
            pictureActionIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            try {
                startActivityForResult(
                    pictureActionIntent,
                    GALLERY_PICTURE
                )
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }

        dialog.setNegativeButton(
            "Camera"
        ) { _, _ ->
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (packageManager?.resolveActivity(takePictureIntent, 0) != null) {
                var photoFile: File? = null
                try {
                    photoFile = createFile()
                } catch (ex: IOException) {
                    Toast.makeText(requireContext(), "Error while creating file", Toast.LENGTH_LONG).show()
                }
                if (photoFile != null) {
                    val photoURI = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.wirelessnoticeboard.fileprovider",
                        photoFile
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, CAMERA_REQUEST)
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Throws(IOException::class)
    private fun createFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            mCurrentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        @Nullable data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == CAMERA_REQUEST) {
            val f = File(mCurrentPhotoPath)
            val imageUri = Uri.fromFile(f)
            image = imageUri
            Glide.with(requireContext()).load(imageUri).into(binding.thumbnail)
        } else if (resultCode == RESULT_OK && requestCode == GALLERY_PICTURE) {
            if (data != null) {
                val selectedImage = data.data
                image = selectedImage
                Glide.with(requireContext()).load(selectedImage).into(binding.thumbnail)
            } else {
                Toast.makeText(
                    requireContext(), "Cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    &&grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(requireContext(),"Permission Denied",Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
            }
        }
    }

    data class Message(val image: String, val type: String, val priority: String, val delay: String)
}

