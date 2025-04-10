package com.example.huddle.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.huddle.R
import com.example.huddle.adapters.ProjectScreenAdapter
import com.example.huddle.adapters.TeamAdapter
import com.example.huddle.data.Project
import com.example.huddle.data.Team
import com.example.huddle.dialogs.AddTeamDialog
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Suppress("NAME_SHADOWING")
class TeamsFragment : Fragment() {
    private lateinit var teamRecyclerView: RecyclerView
    private lateinit var teamShimmerLayout: ShimmerFrameLayout
    private lateinit var teamAdapter: TeamAdapter
    private val teamList = mutableListOf<Team>()
    private lateinit var noResultsTv: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_teams, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        teamShimmerLayout = view.findViewById(R.id.team_shimmer_layout)
        teamShimmerLayout.startShimmer()

        teamRecyclerView = view.findViewById(R.id.team_rv)
        teamRecyclerView.isNestedScrollingEnabled = false
        teamRecyclerView.layoutManager =
            object : LinearLayoutManager(view.context) {
                override fun canScrollVertically() = false
            }
        teamAdapter = TeamAdapter(teamList)
        teamRecyclerView.adapter = teamAdapter

        view.findViewById<MaterialCardView>(R.id.fragment_teams_add).setOnClickListener {
            val addTeamDialog: DialogFragment = AddTeamDialog()
            addTeamDialog.show(parentFragmentManager, "AddTeamDialog")
        }

        val db = FirebaseFirestore.getInstance()
        val user = Firebase.auth.currentUser?.uid.toString()

        updateteamList()

        noResultsTv = view.findViewById(R.id.no_results_tv)

        view.findViewById<TextInputEditText>(R.id.team_search_edt).addTextChangedListener(object :
            TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            @SuppressLint("NotifyDataSetChanged")
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    db.collection("Teams")
                        .orderBy("name", Query.Direction.ASCENDING)
                        .startAt(query)
                        .endAt(query + "\uf8ff")
                        .addSnapshotListener { snapshots, error ->
                            if (error != null) {
                                return@addSnapshotListener
                            }

                            if (snapshots != null) {
                                teamList.clear()
                                for (document in snapshots) {
                                    val teamData = document.toObject(Team::class.java)
                                    if (teamData.members.contains(user)) teamList.add(teamData)
                                }

                                if (teamList.isEmpty()) {
                                    noResultsTv.visibility = View.VISIBLE
                                } else {
                                    noResultsTv.visibility = GONE
                                }

                                teamAdapter.notifyDataSetChanged()
                            }
                        }
                } else {
                    updateteamList()
                }
            }
        })

    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateteamList() {
        val db = FirebaseFirestore.getInstance().collection("Teams")
        val user = Firebase.auth.currentUser?.uid.toString()

        db.addSnapshotListener { snapshots, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (snapshots != null) {
                teamList.clear()
                for (document in snapshots) {
                    val teamData = document.toObject(Team::class.java)
                    if (teamData.members.contains(user)) teamList.add(teamData)
                }

                teamShimmerLayout.stopShimmer()
                teamShimmerLayout.visibility = GONE
                teamRecyclerView.visibility = View.VISIBLE
                teamAdapter.notifyDataSetChanged()

                if (teamList.isEmpty()) {
                    noResultsTv.visibility = View.VISIBLE
                } else {
                    noResultsTv.visibility = GONE
                }
            }
        }
    }
}
