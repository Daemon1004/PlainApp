package com.example.plainapp.ui.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.plainapp.data.User
import com.example.plainapp.databinding.UserPreviewViewBinding
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserPreviewAdapter() : RecyclerView.Adapter<UserPreviewAdapter.UserPreviewViewHolder>(), View.OnClickListener {

    private var data: List<User> = emptyList()

    class UserPreviewViewHolder(val binding: UserPreviewViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPreviewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = UserPreviewViewBinding.inflate(inflater, parent, false)

        return UserPreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserPreviewViewHolder, position: Int) {
        val user = data[position]

        val binding = holder.binding

        binding.userName.text = user.name
        binding.userNickname.text = user.nickname

        binding.root.setOnClickListener(this)

        holder.itemView.tag = position

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newData: List<User>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun onClick(view: View) {

        val user = data[view.tag as Int]

        val intent = Intent(view.context, ProfileActivity::class.java)
        intent.putExtra("data", Json.encodeToString(user))
        view.context.startActivity(intent)

    }

}