package com.example.planeapp.ui.chats

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.planeapp.ChatActivity
import com.example.planeapp.data.Chat
import com.example.planeapp.databinding.ChatPreviewViewBinding


class ChatPreviewAdapter : RecyclerView.Adapter<ChatPreviewAdapter.ChatPreviewViewHolder>(), View.OnClickListener {

    private var data: List<Chat> = emptyList()

    class ChatPreviewViewHolder(val binding: ChatPreviewViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatPreviewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ChatPreviewViewBinding.inflate(inflater, parent, false)

        return ChatPreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatPreviewViewHolder, position: Int) {
        val chat = data[position]
        //val context = holder.itemView.context

        val binding = holder.binding

        binding.name.text = chat.name
        binding.lastMessage.text = "" //chat.lastMessage
        binding.lastTime.text = "" //chat.lastTime

        binding.root.setOnClickListener(this)

        holder.itemView.tag = chat

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newData: List<Chat>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun onClick(view: View) {

        val chat = view.tag as Chat

        view.context.startActivity(Intent(view.context, ChatActivity::class.java))

    }
    
}