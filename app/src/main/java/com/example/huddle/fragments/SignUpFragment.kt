package com.example.huddle.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.huddle.R
import com.example.huddle.activities.BaseHomeActivity
import com.example.huddle.activities.OnBoardingActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("DEPRECATION")
class SignUpFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var progressDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
        dialogBuilder.setView(R.layout.dialog_progress)
            .setCancelable(false).create()

        progressDialog = dialogBuilder.create()

        view.findViewById<MaterialCardView>(R.id.fragment_signup_back)
            ?.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

        val email_edt = view.findViewById<TextInputEditText>(R.id.sign_up_email_edt)
        val password_edt = view.findViewById<TextInputEditText>(R.id.sign_up_password_edt)
        val name_edt = view.findViewById<TextInputEditText>(R.id.sign_up_name_edt)

        view.findViewById<MaterialButton>(R.id.sign_up_btn)?.setOnClickListener {
            if (email_edt.text?.isEmpty() == true || password_edt.text?.isEmpty() == true || name_edt.text?.isEmpty() == true) {
                Toast.makeText(
                    context,
                    "Something went wrong. Please check your details",
                    Toast.LENGTH_LONG
                ).show()
            } else if (password_edt.text?.isEmpty() == false && password_edt.text?.length!! < 8) {
                Toast.makeText(
                    context,
                    "Password must be at least 8 characters long",
                    Toast.LENGTH_LONG
                ).show()
            } else if(!password_edt.text.toString().isStrongPassword()){
                val passDialog = MaterialAlertDialogBuilder(view.context)
                    .setTitle("Password Strength")
                    .setMessage("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()

                passDialog.show()
            } else {
                progressDialog.show()

                auth.createUserWithEmailAndPassword(
                    email_edt?.text.toString(),
                    password_edt?.text.toString()
                )
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser

                            val firestore = FirebaseFirestore.getInstance()
                            val userDocument = firestore.collection("users").document(user?.uid.toString())

                            val userMap = hashMapOf(
                                "id" to user?.uid.toString(),
                                "email" to user?.email.toString(),
                                "name" to name_edt.text.toString(),
                                "profile" to "null"
                            )

                            userDocument.set(userMap)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        startActivity(Intent(activity, BaseHomeActivity::class.java))
                                        activity?.finish()
                                        progressDialog.dismiss()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Sign up unsuccessful. ${task.exception}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        user?.delete()
                                        progressDialog.dismiss()
                                    }
                                }
                        } else {
                            Toast.makeText(
                                activity?.baseContext,
                                "Authentication failed.",
                                Toast.LENGTH_SHORT,
                            ).show()
                            progressDialog.dismiss()
                        }
                    }
            }
        }

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account)
                } catch (e: ApiException) {
                    Log.w("GoogleSignIn", "Google sign in failed", e)
                }
            } else {
                Log.w("GoogleSignIn", "Sign in canceled or failed")
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = activity?.baseContext?.let { GoogleSignIn.getClient(it, gso) }!!

        view.findViewById<MaterialCardView>(R.id.signup_google_card).setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        view.findViewById<MaterialCardView>(R.id.signup_github_card).setOnClickListener {
            githubSignIn()
        }

        view.findViewById<TextView>(R.id.to_sign_in_btn)
            ?.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        progressDialog.show()
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    val firestore = FirebaseFirestore.getInstance()
                    val userDocument = firestore.collection("users").document(user?.uid.toString())

                    val userMap = hashMapOf(
                        "id" to user?.uid.toString(),
                        "email" to user?.email.toString(),
                        "name" to user?.displayName.toString(),
                        "profile" to user?.photoUrl.toString()
                    )

                    userDocument.set(userMap)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                startActivity(Intent(activity, OnBoardingActivity::class.java))
                                activity?.finish()
                                progressDialog.dismiss()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Sign up unsuccessful. ${task.exception}",
                                    Toast.LENGTH_LONG
                                ).show()
                                user?.delete()
                                progressDialog.dismiss()
                            }
                        }
                } else {
                    Toast.makeText(context, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
            }
    }

    private fun githubSignIn() {
        progressDialog.show()
        val provider = OAuthProvider.newBuilder("github.com")

        val pendingResultTask = auth.pendingAuthResult
        if (pendingResultTask != null) {
            pendingResultTask
                .addOnSuccessListener { authResult ->
                    val user = authResult.user

                    val firestore = FirebaseFirestore.getInstance()
                    val userDocument = firestore.collection("users").document(user?.uid.toString())

                    val userMap = hashMapOf(
                        "id" to user?.uid.toString(),
                        "email" to user?.email.toString(),
                        "name" to user?.displayName.toString(),
                        "profile" to user?.photoUrl.toString()
                    )

                    userDocument.set(userMap)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                startActivity(Intent(activity, OnBoardingActivity::class.java))
                                activity?.finish()
                                progressDialog.dismiss()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Sign up unsuccessful. ${task.exception}",
                                    Toast.LENGTH_LONG
                                ).show()
                                user?.delete()
                                progressDialog.dismiss()
                            }
                        }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
        } else {
            auth.startActivityForSignInWithProvider(requireActivity(), provider.build())
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    val database = Firebase.database
                    val myRef = database.getReference("user").child(user?.uid.toString())

                    val hashMap = HashMap<String, String>()
                    hashMap["id"] = user?.uid.toString()
                    hashMap["email"] = user?.email.toString()
                    hashMap["name"] = user?.displayName.toString()
                    hashMap["profile"] = user?.photoUrl.toString()

                    myRef.setValue(hashMap)
                        .addOnCompleteListener({ task1 ->
                            if (task1.isSuccessful) {
                                startActivity(Intent(activity, OnBoardingActivity::class.java))
                                activity?.finish()
                                progressDialog.dismiss()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Sign up unsuccessful. " + task1.exception + "",
                                    Toast.LENGTH_LONG
                                ).show()
                                user?.delete()
                                progressDialog.dismiss()
                            }
                        })
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
        }
    }
}

fun String.isStrongPassword(): Boolean {
    val hasUpperCase = any { it.isUpperCase() }
    val hasLowerCase = any { it.isLowerCase() }
    val hasDigits = any { it.isDigit() }
    val hasSpecialChar = any { !it.isLetterOrDigit() }

    return hasDigits && hasUpperCase && hasLowerCase && hasSpecialChar
}
