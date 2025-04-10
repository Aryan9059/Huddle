package com.example.huddle.dialogs

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.huddle.R
import com.example.huddle.adapters.MemberAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.TimeZone

@Suppress("NAME_SHADOWING")
class AddProjectDialog : DialogFragment() {
    private lateinit var memberRv: RecyclerView
    private val memberList = mutableListOf<String>()
    private lateinit var memberAdapter: MemberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_project, container, false)
    }

    @SuppressLint("NotifyDataSetChanged", "UseCompatLoadingForDrawables", "DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialCardView>(R.id.dialog_add_project_back).setOnClickListener {
            dialog?.dismiss()
        }

        val dropdownOptions = mutableListOf<Pair<String, String>>()

        val selectProjectEdt = view.findViewById<MaterialAutoCompleteTextView>(R.id.select_project_edt)
        var selectedProjectId = arguments?.getString("selectedProjectId")
        var listF: MutableList<String> = mutableListOf()
        val addProjectBtn = view.findViewById<MaterialButton>(R.id.save_project_btn)
        val addMemberBtn = view.findViewById<MaterialCardView>(R.id.add_member_project)

        memberRv = view.findViewById(R.id.project_member_rv)
        memberRv.isNestedScrollingEnabled = false
        memberRv.layoutManager =
            object : LinearLayoutManager(view.context, HORIZONTAL, false) {
                override fun canScrollVertically() = false
            }

        memberAdapter = MemberAdapter(memberList)
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            memberList.add(userId)
        }
        memberRv.adapter = memberAdapter

        addMemberBtn.setOnClickListener {
            val dialog = SearchUserDialog.newInstance(ArrayList(memberList))
            val args = Bundle()
            if(listF.isNotEmpty()) args.putStringArrayList("memberList", ArrayList(listF))
            dialog.arguments = args
            dialog.setOnUsersSelectedListener { selectedUserIds ->
                memberList.clear()
                memberList.addAll(selectedUserIds)
                memberAdapter.notifyDataSetChanged()
            }
            dialog.show(parentFragmentManager, "UserSelectionDialog")
        }

        val colors = listOf("#0a0c16","#03A1B6", "#4CAF50", "#9C27B0", "#3580ff", "#FF5722")
        var selectedColor: String? = "#0a0c16"

        val colorViews = listOf(
            view.findViewById<ImageView>(R.id.label_default_iv),
            view.findViewById(R.id.label_cyan_iv),
            view.findViewById(R.id.label_green_iv),
            view.findViewById(R.id.label_magenta_iv),
            view.findViewById(R.id.label_accent_iv),
            view.findViewById(R.id.label_orange_iv)
        )

        colorViews.forEachIndexed { index, view ->
            view.setOnClickListener {
                selectedColor = colors[index]
                colorViews.forEach {
                    it.setImageResource(R.color.transparent)
                    it.background = null
                }
                view.background = context?.getDrawable(R.drawable.label_selected_bg)
                view.setImageResource(R.drawable.tick_square)
            }
        }

        val nameEdt = view.findViewById<TextInputEditText>(R.id.add_project_name)
        val descEdt = view.findViewById<TextInputEditText>(R.id.add_project_desc)

        val startDateEdt = view.findViewById<TextInputEditText>(R.id.start_date_edt)
        startDateEdt.setOnClickListener {
            val constraintsBuilder = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())

            val dialogs: MaterialDatePicker<*> = MaterialDatePicker.Builder.datePicker().setCalendarConstraints(constraintsBuilder.build()).setTitleText("Select Date").build()

            dialogs.show(requireActivity().supportFragmentManager, "tag")
            dialogs.addOnPositiveButtonClickListener { selection ->
                @SuppressLint("SimpleDateFormat") val format =
                    SimpleDateFormat("dd/MM/yyyy")
                val calendar = Calendar.getInstance(TimeZone.getDefault())
                calendar.timeInMillis = selection.toString().toLong()
                var myDate = format.format(calendar.time)
                val formatter =
                    DateTimeFormatter.ofPattern("dd-MM-yyyy'T'HH:mm:ss")
                myDate = myDate.replace("/", "-") + "T00:00:00"
                val dateTime = LocalDateTime.parse(myDate, formatter)
                val formatter1 =
                    DateTimeFormatter.ofPattern("MMMM d, yyyy")
                startDateEdt.setText(dateTime.format(formatter1))
            }
        }

        val endDateEdt = view.findViewById<TextInputEditText>(R.id.end_date_edt)
        endDateEdt.setOnClickListener {
            val constraintsBuilder = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
            val dialogs: MaterialDatePicker<*> =
                MaterialDatePicker.Builder.datePicker().setCalendarConstraints(constraintsBuilder.build()).setTitleText("Select Date").build()
            dialogs.show(requireActivity().supportFragmentManager, "tag")
            dialogs.addOnPositiveButtonClickListener { selection ->
                @SuppressLint("SimpleDateFormat") val format =
                    SimpleDateFormat("dd/MM/yyyy")
                val calendar = Calendar.getInstance(TimeZone.getDefault())
                calendar.timeInMillis = selection.toString().toLong()
                var myDate = format.format(calendar.time)
                val formatter =
                    DateTimeFormatter.ofPattern("dd-MM-yyyy'T'HH:mm:ss")
                myDate = myDate.replace("/", "-") + "T00:00:00"
                val dateTime = LocalDateTime.parse(myDate, formatter)
                val formatter1 =
                    DateTimeFormatter.ofPattern("MMMM d, yyyy")
                endDateEdt.setText(dateTime.format(formatter1))
            }
        }

        FirebaseFirestore.getInstance().collection("Teams")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val option = document.getString("name")
                    val documentId = document.id
                    if(!dropdownOptions.contains(Pair(option, documentId))) option?.let { dropdownOptions.add(Pair(it, documentId)) }
                }

                if(selectedProjectId != null) {
                    selectProjectEdt.setText(dropdownOptions.find { it.second == selectedProjectId }?.first.toString())
                    Firebase.firestore.collection("Teams").document(selectedProjectId!!).get().addOnSuccessListener {
                        listF = it["users"] as MutableList<String>
                    }
                } else {
                    addProjectBtn.isEnabled = false
                    addMemberBtn.isEnabled = false
                    descEdt.isEnabled = false
                    startDateEdt.isEnabled = false
                    endDateEdt.isEnabled = false
                }

                val projectNames = dropdownOptions.map { it.first }

                val adapter = ArrayAdapter(
                    view.context,
                    R.layout.dropdown_item,
                    R.id.textViewDropdownItem,
                    projectNames
                )

                selectProjectEdt.setAdapter(adapter)
            }
            .addOnFailureListener { exception ->
                Log.e("FireStore", "Error fetching data: ", exception)
            }

        selectProjectEdt.setOnItemClickListener { parent, _, position, _ ->
            val selectedOption = parent.getItemAtPosition(position).toString()
            selectedProjectId = dropdownOptions.find { it.first == selectedOption }?.second.toString()
            addProjectBtn.isEnabled = true
            addMemberBtn.isEnabled = true
            descEdt.isEnabled = true
            startDateEdt.isEnabled = true
            endDateEdt.isEnabled = true
            Firebase.firestore.collection("Teams").document(selectedProjectId!!).get().addOnSuccessListener {
                listF = it["members"] as MutableList<String>
            }
        }

        addProjectBtn.setOnClickListener {
            if(!(nameEdt.text.toString().isEmpty() || descEdt.text.toString().isEmpty() || startDateEdt.text.toString().isEmpty()
                || endDateEdt.text.toString().isEmpty())) {
                val firestore = FirebaseFirestore.getInstance()
                val userDocument = firestore.collection("Project").document()

                val projectMap = hashMapOf(
                    "projectId" to userDocument.id,
                    "projectDesc" to descEdt.text.toString(),
                    "projectName" to nameEdt.text.toString(),
                    "startDate" to startDateEdt.text.toString(),
                    "endDate" to endDateEdt.text.toString(),
                    "tasks" to emptyList<String>(),
                    "color" to selectedColor,
                    "users" to memberList,
                    "favourite" to emptyList<String>(),
                    "team" to selectedProjectId,
                )

                userDocument.set(projectMap)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            dialog?.dismiss()
                        } else {
                            Toast.makeText(
                                context,
                                "Upload Failed. ${task.exception}",
                                Toast.LENGTH_LONG
                            ).show()
                            dialog?.dismiss()
                        }
                    }
                dialog?.dismiss()
            } else {
                Toast.makeText(context, "Please fill all the fields", Toast.LENGTH_LONG).show()
            }
        }
    }
}