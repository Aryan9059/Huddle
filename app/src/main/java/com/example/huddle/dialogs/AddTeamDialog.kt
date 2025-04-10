package com.example.huddle.dialogs

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView
import com.example.huddle.R
import com.example.huddle.adapters.MemberAdapter
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream

class AddTeamDialog : DialogFragment() {
    private lateinit var memberRv: RecyclerView
    private val memberList = mutableListOf<String>()
    private lateinit var memberAdapter: MemberAdapter
    private var imageUri: String = ""
    private lateinit var uploadTask: StorageTask<UploadTask.TaskSnapshot>

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            view?.findViewById<ImageView>(R.id.add_team_logo_iv)?.setImageURI(uri)

            val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            dialogBuilder.setView(R.layout.dialog_progress_pic)
                .setCancelable(false)

            val pd = dialogBuilder.create()
            pd.show()

            val storageReference = Firebase.storage.reference
            val fileReference: StorageReference =
                storageReference.child("huddle/teams/${System.currentTimeMillis()}.jpg")

            uploadTask = fileReference.putFile(uri)
            uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                return@Continuation fileReference.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    imageUri = downloadUri.toString()
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
            } else {
                Log.d("PhotoPicker", "No media selected")
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
        return inflater.inflate(R.layout.dialog_add_team, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val teamName = view.findViewById<TextInputEditText>(R.id.team_name_edt)
        val addMemberBtn = view.findViewById<MaterialCardView>(R.id.add_member_btn)

        memberRv = view.findViewById(R.id.project_member_rv)
        memberRv.isNestedScrollingEnabled = false
        memberRv.layoutManager =
            object : LinearLayoutManager(view.context, HORIZONTAL, false) {
                override fun canScrollVertically() = false
            }

        memberAdapter = MemberAdapter(memberList)
        memberRv.adapter = memberAdapter
        val currentUser = Firebase.auth.currentUser?.uid.toString()

        addMemberBtn.setOnClickListener {
            val dialog = SearchUserDialog.newInstance(ArrayList(memberList))
            val args = Bundle()
            dialog.arguments = args
            dialog.setOnUsersSelectedListener { selectedUserIds ->
                memberList.clear()
                memberList.addAll(selectedUserIds)
                memberAdapter.notifyDataSetChanged()
            }
            dialog.show(parentFragmentManager, "UserSelectionDialog")
        }

        view.findViewById<MaterialCardView>(R.id.add_team_image_cv).setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        view.findViewById<MaterialCardView>(R.id.dialog_add_team_back).setOnClickListener {
            dialog?.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.create_team_btn).setOnClickListener {
            val firestore = FirebaseFirestore.getInstance()
            val userDocument = firestore.collection("Teams").document()

            val teamMap = hashMapOf(
                "teamId" to userDocument.id,
                "name" to teamName.text.toString(),
                "members" to memberList,
                "image" to if (imageUri != "") imageUri else "1",
                "admin" to mutableListOf<String>(currentUser),
            )

            userDocument.set(teamMap)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        for (member in memberList) {
                            firestore.collection("users").document(member).get().addOnSuccessListener {
                                if(it["teams"] == null) {

                                    val list = mutableListOf<String>()
                                    list.add(userDocument.id)
                                    firestore.collection("users").document(member).update("teams", list).addOnSuccessListener {

                                    }
                                } else {
                                    val list = it["teams"] as MutableList<String>
                                    list.add(userDocument.id)
                                    firestore.collection("users").document(member).update("teams", list)
                                }
                                dialog?.dismiss()
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Upload Failed. ${task.exception}",
                            Toast.LENGTH_LONG
                        ).show()
                        dialog?.dismiss()
                    }
                }
        }
    }
}