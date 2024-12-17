package com.example.planeapp.ui.chats

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.planeapp.ChatActivity
import com.example.planeapp.databinding.ChatPreviewViewBinding


class ChatPreviewAdapter : RecyclerView.Adapter<ChatPreviewAdapter.ChatPreviewViewHolder>(), View.OnClickListener {

    var data: List<ChatPreviewData> = emptyList()

    class ChatPreviewViewHolder(val binding: ChatPreviewViewBinding) : RecyclerView.ViewHolder(binding.root)
    
    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatPreviewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ChatPreviewViewBinding.inflate(inflater, parent, false)

        return ChatPreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatPreviewViewHolder, position: Int) {
        val chatPreview = data[position]
        //val context = holder.itemView.context

        val binding = holder.binding

        binding.userName.text = chatPreview.name
        binding.lastMessage.text = chatPreview.lastMessage
        binding.lastTime.text = chatPreview.lastTime

        binding.root.setOnClickListener(this)

        holder.itemView.tag = chatPreview

    }

    override fun onClick(view: View) {

        val chatPreview = view.tag as ChatPreviewData

        view.context.startActivity(Intent(view.context, ChatActivity::class.java))

    }
    
}