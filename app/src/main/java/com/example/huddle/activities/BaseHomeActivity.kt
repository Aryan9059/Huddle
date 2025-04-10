package com.example.huddle.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.huddle.R
import com.example.huddle.dialogs.AddProjectDialog
import com.example.huddle.dialogs.AddTaskDialog
import com.example.huddle.dialogs.AddTeamDialog
import com.example.huddle.fragments.CommunityFragment
import com.example.huddle.fragments.HomeFragment
import com.example.huddle.fragments.ProjectFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import com.example.huddle.fragments.TeamsFragment

class BaseHomeActivity : AppCompatActivity() {
    private lateinit var currentFragment: Fragment

    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_home)

        val sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        val isNightMode = sharedPreferences.getBoolean("isNightMode", false)

        val navSp = getSharedPreferences("navigation", MODE_PRIVATE)
        val navItem = navSp.getString("nav_item", "Home")

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                } else {
                    Log.v("Tag", "Not Subscribed.")
                }
            }

        fun askForNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                    }

                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            askForNotificationPermission()
        }, 2000)

        val window = window
        if (isNightMode) {
            window.decorView.windowInsetsController?.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS)
        } else {
            window.decorView.windowInsetsController?.setSystemBarsAppearance(APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS)
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val addButton: MaterialCardView = findViewById(R.id.AddButton)

        when (navItem) {
            "Community" -> {
                bottomNavigationView.selectedItemId = R.id.navigation_item4
            }
            "Projects" -> {
                bottomNavigationView.selectedItemId = R.id.navigation_item2
            }
            "My Teams" -> {
                bottomNavigationView.selectedItemId = R.id.navigation_item3
            }
            else -> {
                bottomNavigationView.selectedItemId = R.id.navigation_item1
            }
        }

        currentFragment = if(navItem == "My Teams") TeamsFragment() else if(navItem == "Project") ProjectFragment() else if(navItem == "Community") CommunityFragment() else HomeFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, currentFragment, navItem)
            .commit()

        bottomNavigationView.setOnItemSelectedListener { item ->

            navSp.edit {
                putString("nav_item", item.title.toString())
            }

            val newFragment = when (item.itemId) {
                R.id.navigation_item1 -> getOrCreateFragment("HOME", HomeFragment())
                R.id.navigation_item2 -> getOrCreateFragment("PROJECT", ProjectFragment())
                R.id.navigation_item4 -> getOrCreateFragment("COMMUNITY", CommunityFragment())
                R.id.navigation_item3 -> getOrCreateFragment("MY TEAMS", TeamsFragment())
                else -> currentFragment
            }

            switchFragment(newFragment)
            true
        }

        addButton.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun getOrCreateFragment(tag: String, fragment: Fragment): Fragment {
        val fragmentManager = supportFragmentManager
        return fragmentManager.findFragmentByTag(tag) ?: fragment
    }

    private fun switchFragment(fragment: Fragment) {
        if (currentFragment != fragment) {
            val transaction = supportFragmentManager.beginTransaction()

            transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_in_left
            )

            currentFragment.let { transaction.hide(it) }

            if (fragment.isAdded) {
                transaction.show(fragment)
            } else {
                transaction.add(R.id.nav_host_fragment, fragment, fragment.javaClass.simpleName)
            }

            transaction.commit()
            currentFragment = fragment
        }
    }

    private fun showBottomSheet() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.bottom_sheet_layout)

        val closeButton: MaterialCardView = dialog.findViewById(R.id.imageViewClose)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            FirebaseMessaging.getInstance().subscribeToTopic("all")

            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result

                    Firebase.firestore.collection("users").document(Firebase.auth.currentUser?.uid.toString()).update("token", token)
                }
            }
        }, 2000)

        dialog.findViewById<RelativeLayout?>(R.id.relativeLayoutCreateTask).setOnClickListener {
            val addTaskDialog: DialogFragment = AddTaskDialog()
            addTaskDialog.show(supportFragmentManager, "AddTaskDialog")
            dialog.dismiss()
        }

        dialog.findViewById<RelativeLayout?>(R.id.relativeLayoutCreateTeam).setOnClickListener {
            val addTaskDialog: DialogFragment = AddTeamDialog()
            addTaskDialog.show(supportFragmentManager, "AddTeamDialog")
            dialog.dismiss()
        }

        dialog.findViewById<RelativeLayout?>(R.id.relativeLayoutCreateProject)?.setOnClickListener {
            val addProjectDialog: DialogFragment = AddProjectDialog()
            addProjectDialog.show(supportFragmentManager, "AddProjectDialog")
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun status(status: Long) {
        val user = Firebase.auth.currentUser?.uid.toString()
        Firebase.firestore.collection("users").document(user).update("lastSeen", status)
    }

    override fun onResume() {
        super.onResume()
        status(0)
    }

    override fun onPause() {
        super.onPause()
        status(Calendar.getInstance().timeInMillis)
    }
}
