package com.example.huddle.dialogs

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.huddle.R
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage

class EditProfileDialog : DialogFragment() {
    private lateinit var imageUri: String
    private lateinit var uploadTask: StorageTask<UploadTask.TaskSnapshot>

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            view?.findViewById<ImageView>(R.id.edt_profile_picture)?.setImageURI(uri)

            val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            dialogBuilder.setView(R.layout.dialog_progress_pic)
                .setCancelable(false)

            val pd = dialogBuilder.create()
            pd.show()

            val storageReference = Firebase.storage.reference
            val fileReference: StorageReference =
                storageReference.child("huddle/profile/${System.currentTimeMillis()}.jpg")

            uploadTask = fileReference.putFile(uri)
            uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                return@Continuation fileReference.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    val mUri = downloadUri.toString()

                    val reference = Firebase.firestore.collection("users").document(Firebase.auth.currentUser?.uid ?: "")
                    val map = HashMap<String, Any>()
                    map["profile"] = mUri
                    reference.update(map)
                } else {
                    Toast.makeText(requireContext(), "Failed!", Toast.LENGTH_SHORT).show()
                }
                pd.dismiss()
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                pd.dismiss()
            }

            if (uploadTask.isInProgress) {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    0
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_profile, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = arguments?.getString("name")
        val phone = arguments?.getString("phone")
        val photo = arguments?.getString("photo")

        imageUri = photo ?: "null"

        val nameTv = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edt_profile_name)
        nameTv.setText(name)
        val phoneTv = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edt_profile_phone)
        phoneTv.setText(phone)

        val user = Firebase.auth.currentUser

        try {
            Glide.with(requireContext())
                .load(photo)
                .into(view.findViewById(R.id.edt_profile_picture))
        } catch(_: Exception) {}

        view.findViewById<MaterialCardView>(R.id.edit_profile_picture_cv).setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        view.findViewById<MaterialCardView>(R.id.dialog_edit_profile_back).setOnClickListener {
            dialog?.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.save_profile_btn).setOnClickListener {
            val updatePath = user?.uid?.let { it1 ->
                Firebase.firestore.collection("users").document(
                    it1
                )
            }

            val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            dialogBuilder.setView(R.layout.dialog_progress)
                .setCancelable(false).create()

            val progressDialog = dialogBuilder.create()
            progressDialog.show()

            if (nameTv.text.toString().isEmpty()) {
                nameTv.error = "Name cannot be empty"
                progressDialog.dismiss()
            } else {
                updatePath?.update("name", nameTv.text.toString())
                if (phoneTv.text.toString().isNotEmpty()) updatePath?.update("phone", phoneTv.text.toString())
                progressDialog.dismiss()
                dialog?.dismiss()
            }
        }

        view.findViewById<MaterialButton>(R.id.cancel_profile_btn).setOnClickListener {
            dialog?.dismiss()
        }
    }
}