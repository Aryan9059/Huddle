package com.example.huddle.adapters

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.huddle.R
import com.example.huddle.data.Task
import com.example.huddle.data.Team
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView

class TeamAdapter(private val teams: List<Team>) : RecyclerView.Adapter<TeamAdapter.TeamViewHolder>() {

    class TeamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val teamTitle: TextView = itemView.findViewById(R.id.team_title)
        val teamMembers: TextView = itemView.findViewById(R.id.team_members)
        val teamImage: CircleImageView = itemView.findViewById(R.id.team_img)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.team_item, parent, false)
        return TeamViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        val team = teams[position]
        holder.teamTitle.text = team.name
        holder.teamMembers.text = team.members.size.toString() + " Members"
        Glide.with(holder.itemView.context).load(team.image).into(holder.teamImage)
    }

    override fun getItemCount(): Int = teams.size
}
