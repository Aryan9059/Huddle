package com.example.huddle.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.huddle.R
import com.example.huddle.data.Task
import com.example.huddle.dialogs.EditProfileDialog
import com.example.huddle.utility.decodeBase64ToBitmap
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        val isNightMode = sharedPreferences.getBoolean("isNightMode", false)

        val window = window
        if (isNightMode) {
            window.decorView.windowInsetsController?.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS)
        } else {
            window.decorView.windowInsetsController?.setSystemBarsAppearance(
                APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS
            )
        }

        findViewById<MaterialCardView>(R.id.activity_profile_back).setOnClickListener {
            finish()
        }

        val profileNameTv = findViewById<TextView>(R.id.profile_name_tv)
        val profileEmailTv = findViewById<TextView>(R.id.profile_email_tv)

        val user = Firebase.auth.currentUser
        val userDocument = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user?.uid.toString())

        var taskOnGoing: Int
        var taskComplete: Int

        Firebase.firestore.collection("Task")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    taskOnGoing = 0
                    taskComplete = 0
                    for (document in snapshots) {
                        val task = document.toObject(Task::class.java)
                        if(task.users.contains(user?.uid) && task.status == 1) {
                            taskOnGoing++
                        }
                        if(task.users.contains(user?.uid) && task.status == 2) {
                            taskComplete++
                        }
                    }

                    findViewById<TextView>(R.id.on_going_task).text = taskOnGoing.toString()
                    findViewById<TextView>(R.id.complete_task).text = taskComplete.toString()
                }
            }

        val profilePic = findViewById<ImageView>(R.id.profile_pic)

        profileNameTv.text = "Loading..."
        profileEmailTv.text = "Loading..."

        userDocument.addSnapshotListener { documentSnapshot, error ->
            if (error != null) {
                profileNameTv.text = "Error Loading Data"
                profileEmailTv.text = "Error Loading Data"
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                val name = documentSnapshot.getString("name")
                val email = documentSnapshot.getString("email")
                val profileUrl = documentSnapshot.getString("profile")
                val phoneNumber = documentSnapshot.getString("phone")
                var profile64 = ""
                if (profileUrl == "1") profile64 = documentSnapshot.getString("profile_64")!!

                profileNameTv.text = name ?: "N/A"
                profileEmailTv.text = email ?: "N/A"

                findViewById<MaterialButton>(R.id.profile_edit_btn).setOnClickListener {
                    val addTaskDialog: DialogFragment = EditProfileDialog()
                    val bundle = Bundle()
                    bundle.putString("name", name)
                    bundle.putString("phone", phoneNumber)
                    bundle.putString("photo", profileUrl)
                    if (profileUrl == "1") bundle.putString("photo_64", profile64)
                    addTaskDialog.arguments = bundle
                    addTaskDialog.show(this.supportFragmentManager, "EditProfileDialog")
                }

                if (!profileUrl.isNullOrEmpty() && profileUrl != "null" && profileUrl != "1") {
                    try {
                        Glide.with(this)
                            .load(profileUrl)
                            .into(profilePic)
                    } catch(e: Exception) {
                        Log.e("ProfileFragment", "Error loading profile picture: ${e.message}")
                    }
                } else if (profileUrl == "1") {
                    val profile64 = documentSnapshot.getString("profile_64")
                    profilePic.setImageBitmap(profile64?.let { decodeBase64ToBitmap(it) })
                }
            } else {
                profileNameTv.text = "No Data Found"
                profileEmailTv.text = "No Data Found"
            }
        }

        findViewById<RelativeLayout>(R.id.sign_out_rl).setOnClickListener {
            val passDialog = MaterialAlertDialogBuilder(this)
                .setTitle("Sign out")
                .setMessage("Do you want to sign out your account from the app?")
                .setPositiveButton("OK") { dialog, _ ->
                    Firebase.firestore.collection("users").document(user?.uid.toString()).update("lastSeen", System.currentTimeMillis())
                    Firebase.auth.signOut()

                    if(Firebase.auth.currentUser?.providerId.equals("google.com")){
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()

                        googleSignInClient =
                            this.let { GoogleSignIn.getClient(it, gso) }

                        googleSignInClient.signOut()
                        googleSignInClient.revokeAccess()
                    }

                    val navSp = getSharedPreferences("navigation", MODE_PRIVATE)
                    val editor = navSp?.edit()
                    editor?.putString("nav_item", "Home")
                    editor?.apply()
                    dialog.dismiss()

                    finish()
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            passDialog.show()
        }

        findViewById<RelativeLayout>(R.id.settings_layout).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}