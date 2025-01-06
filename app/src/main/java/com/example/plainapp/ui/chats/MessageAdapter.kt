package com.example.plainapp.ui.chats

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.plainapp.R
import com.example.plainapp.copyToClipboard
import com.example.plainapp.data.Message
import com.example.plainapp.databinding.MessageViewBinding
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class MessageAdapter(private val chatActivity: ChatActivity) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>(), View.OnClickListener {

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

        val linearLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)

        if (message.createdBy != chatActivity.myUser!!.id) {

            linearLayoutParams.gravity = Gravity.START
            binding.loadImg.visibility = View.GONE

        } else {

            linearLayoutParams.gravity = Gravity.END
            binding.loadImg.visibility = if (message.notifyDate != null) View.VISIBLE else View.GONE

        }

        binding.message.layoutParams = linearLayoutParams
        binding.messagePlate.layoutParams = linearLayoutParams

        val time = LocalDateTime.ofInstant(Instant.parse(message.createdAt), OffsetDateTime.now().offset)
            .format(DateTimeFormatter.ofPattern("HH:mm"))

        binding.message.text = message.body
        binding.time.text = time

        binding.root.setOnClickListener(this)

        holder.itemView.tag = message

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newData: List<Message>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun onClick(view: View) {

        if (chatActivity.myUser == null) return

        val message = view.tag as Message

        val popupMenu = PopupMenu(chatActivity, (view as LinearLayout).findViewById(R.id.messagePlate))
        popupMenu.inflate(if (message.createdBy == chatActivity.myUser!!.id) R.menu.popup_my_message_menu else R.menu.popup_message_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu1 -> {
                    copyToClipboard(chatActivity, message.body)
                    return@setOnMenuItemClickListener true
                }
                R.id.menu2 -> {
                    TODO("edit message")
                    @Suppress("UNREACHABLE_CODE")
                    return@setOnMenuItemClickListener true
                }
                R.id.menu3 -> {
                    TODO("delete message")
                    @Suppress("UNREACHABLE_CODE")
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()

    }

}