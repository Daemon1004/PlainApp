package com.example.plainapp.ui.chats

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.plainapp.data.Message
import com.example.plainapp.databinding.MessageViewBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>(), View.OnClickListener {

    private var data: List<Message> = emptyList()

    class MessageViewHolder(val binding: MessageViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = MessageViewBinding.inflate(inflater, parent, false)

        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = data[position]

        val binding = holder.binding

        binding.message.text = message.text
        binding.time.text = Instant.ofEpochMilli(message.time).
            atZone(ZoneId.systemDefault()).toLocalDateTime().
            format(DateTimeFormatter.ofPattern( "HH:mm" ))

        binding.root.setOnClickListener(this)

        holder.itemView.tag = message

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newData: List<Message>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun onClick(view: View) {

        //val message = view.tag as Message



    }

}