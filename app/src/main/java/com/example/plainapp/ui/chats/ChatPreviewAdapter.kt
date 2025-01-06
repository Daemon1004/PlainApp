package com.example.plainapp.ui.chats

import android.annotation.SuppressLint
import android.content.Intent
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.plainapp.ChatActivity
import com.example.plainapp.MainActivity
import com.example.plainapp.data.Chat
import com.example.plainapp.data.ChatViewModel
import com.example.plainapp.data.observeOnce
import com.example.plainapp.databinding.ChatPreviewViewBinding
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


class ChatPreviewAdapter(
    private val chatViewModel: ChatViewModel,
    private val viewLifecycleOwner: LifecycleOwner,
    private val mainActivity: MainActivity
) : RecyclerView.Adapter<ChatPreviewAdapter.ChatPreviewViewHolder>(), View.OnClickListener {

    private var data: List<Chat> = emptyList()

    class ChatPreviewViewHolder(val binding: ChatPreviewViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = data.size

    private var timer: CountDownTimer ?= null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        timer = object : CountDownTimer(60000, 1000) {
            @SuppressLint("NotifyDataSetChanged")
            override fun onTick(millisUntilFinished: Long) { notifyDataSetChanged() }
            override fun onFinish() { start() }
        }
        (timer as CountDownTimer).start()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        if (timer != null) (timer as CountDownTimer).cancel()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatPreviewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ChatPreviewViewBinding.inflate(inflater, parent, false)

        return ChatPreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatPreviewViewHolder, position: Int) {
        val chat = data[position]
        //val context = holder.itemView.context

        val binding = holder.binding

        Log.d("debug", "ChatPreviewAdapter chat preview $chat")
        Log.d("debug", "ChatPreviewAdapter service ${mainActivity.serviceLiveData}")
        Log.d("debug", "ChatPreviewAdapter myUser ${mainActivity.serviceLiveData.value?.userLiveData}")

        val service = mainActivity.serviceLiveData.value!!
        val myUser = service.userLiveData.value!!
        val participant = if (chat.participant1 == myUser.id) chat.participant2 else chat.participant1
        chatViewModel.readUser(participant).observeOnce(viewLifecycleOwner) { user ->
            binding.name.text = user.name
        }

        binding.lastMessage.text = ""
        binding.lastTime.text = ""

        binding.loadImg.visibility = View.INVISIBLE

        chatViewModel.readLastChatMessage(chat.id).observe(viewLifecycleOwner) { message ->

            if (message != null) {

                val time = LocalDateTime.ofInstant(Instant.parse(message.createdAt), OffsetDateTime.now().offset)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))

                binding.lastMessage.text = message.body
                binding.lastTime.text = time
                binding.loadImg.visibility = if (message.createdBy == myUser.id) View.VISIBLE else View.INVISIBLE

            }

        }

        binding.onlineCircle.visibility = if (service.isUserOnline(participant)) View.VISIBLE else View.GONE

        binding.root.setOnClickListener(this)

        holder.itemView.tag = chat.id

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newData: List<Chat>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun onClick(view: View) {

        val chatId = view.tag as Long

        val intent = Intent(view.context, ChatActivity::class.java)

        intent.putExtra("chatId", chatId)

        view.context.startActivity(intent)

    }
    
}