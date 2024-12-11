package com.example.planeapp.ui.chats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.planeapp.databinding.ChatPreviewViewBinding

class ChatPreviewAdapter : RecyclerView.Adapter<ChatPreviewAdapter.ChatPreviewViewHolder>() {

    var data: List<ChatPreviewData> = emptyList()
        set(newValue) {
            field = newValue
        }

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

        with(holder.binding) {

            userName.text = chatPreview.name
            lastMessage.text = chatPreview.lastMessage
            lastTime.text = chatPreview.lastTime

        }
    }
    
}