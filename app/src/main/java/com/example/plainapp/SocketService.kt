package com.example.plainapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.plainapp.data.Chat
import com.example.plainapp.data.ChatRepository
import com.example.plainapp.data.LocalDatabase
import com.example.plainapp.data.Message
import com.example.plainapp.data.User
import com.example.plainapp.ui.calls.CallActivity
import com.example.plainapp.ui.chats.ChatActivity
import com.example.plainapp.webrtc.SignalingCommand
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.coroutines.EmptyCoroutineContext


class SocketService : LifecycleService() {

    private val scope = CoroutineScope( EmptyCoroutineContext )
    lateinit var mSocket: Socket
    private lateinit var repository: ChatRepository

    var userLiveData: MutableLiveData<User?> = MutableLiveData<User?>()

    fun logIn(phoneNumber: String) {

        mSocket.emit("userByPN", phoneNumber)
        mSocket.once("userByPN") { args -> scope.launch {

            val user = Json.decodeFromString<User>(args[0].toString())
            scope.launch { userLiveData.postValue(user) }.join()

            Log.d("debug", "Get user: $user")

            val file = File(filesDir, userFileName)
            if (!file.exists()) file.createNewFile()
            else if (file.isDirectory) {
                file.delete()
                file.createNewFile()
            }
            FileOutputStream(file).write(Json.encodeToString(user).toByteArray())

            signIn { updateAll() }

        } }
    }

    private fun signIn(callback: () -> Unit) {

        Log.d("debug", "SignIn")

        mSocket.emit("signin", userLiveData.value!!.id)
        mSocket.once("signin") { signInArgs ->
            val result = signInArgs[0] as String
            if (result == "OK") {

                Log.d("debug", "SignIn OK")

                callback()

            } else {

                Log.d("debug", "SignIn error")

                signIn(callback)

            }
        }

    }

    private fun updateAll() {

        updateChats()

    }

    var updatingChatsStatus = MutableLiveData(false)

    private fun updateChats() {

        if (updatingChatsStatus.value == true) {

            Log.d("debug", "Updating chats: tried to invoke new call")

            return

        }

        updatingChatsStatus.postValue(true)

        Log.d("debug", "Updating chats: started")

        mSocket.emit("myChats")
        mSocket.once("myChats") { myChatsArgs ->

            Log.d("debug", "Updating chats: get chats")

            val chats = Json.decodeFromString<List<Chat>>(myChatsArgs[0].toString())

            val userIds = mutableListOf<Long>()
            for (chat in chats) {
                when (userLiveData.value!!.id) {
                    chat.participant1 -> {
                        userIds.add(chat.participant2)
                    }
                    chat.participant2 -> {
                        userIds.add(chat.participant1)
                    }
                    else -> { throw Exception("Invalid chat: $chat") }
                }
            }

            mSocket.emit("getUsers", JSONArray(userIds))
            mSocket.once("getUsers") { getUsersArgs -> scope.launch {

                Log.d("debug", "Updating chats: get users")

                scope.launch { repository.addUsers(Json.decodeFromString<List<User>>(getUsersArgs[0].toString())) }.join()

                scope.launch { withContext(Dispatchers.Main) {
                    repository.readAllChats.observeOnce(this@SocketService) { localChats -> scope.launch {

                        Log.d("debug", "Updating chats: get local chats")

                        val chatIds = mutableListOf<Long>()

                        val chatsToDelete = emptyList<Chat>().toMutableList()
                        for (lChat in localChats) {
                            if (!chats.contains(lChat))
                                chatsToDelete += lChat
                        }

                        if (chatsToDelete.isNotEmpty()) { scope.launch { repository.deleteChats(chatsToDelete) }.join() }

                        val chatsToAdd = emptyList<Chat>().toMutableList()
                        for (chat in chats) {
                            chatIds += chat.id
                            if (!localChats.contains(chat))
                                chatsToAdd += chat
                        }

                        if (chatsToAdd.isNotEmpty()) { scope.launch { repository.addChats(chatsToAdd) }.join() }

                        mSocket.emit("getChatMessages", JSONArray(chatIds))
                        mSocket.once("getChatMessages") { getChatMessagesArgs -> scope.launch {

                            Log.d("debug", "Updating chats: get messages")

                            val messagesInChats = Json.decodeFromString<List<List<JsonElement>>>(getChatMessagesArgs[0].toString())
                            for(i in chats.indices) {
                                val chat = chats[i]
                                for (messageJson in messagesInChats[i]) {

                                    val message = Json.decodeFromString<Message>(messageJson.toString())

                                    scope.launch {
                                        scope.launch { repository.addChatMessage(chat.id, message) }.join()
                                        Log.d("debug", "Chat message $message in ${chat.id} chat")
                                    }.join()

                                }
                            }

                            updatingChatsStatus.postValue(false)

                            Log.d("debug", "Updating chats: finished")

                        } }

                    } }
                } }


            } }

        }

    }

    private fun updateNewMessages(chats: List<Chat>) {

        val chatIds = mutableListOf<Long>()
        for (chat in chats) chatIds += chat.id

        mSocket.emit("newChatMessages", JSONArray(chatIds))
        mSocket.once("newChatMessages") { newChatMessagesArgs -> scope.launch {

            Log.d("debug", "Updating chats: get new chat messages")

            val messagesInChats = Json.decodeFromString<List<List<JsonElement>>>(newChatMessagesArgs[0].toString())
            for(i in chats.indices) {
                val chat = chats[i]
                for (messageJson in messagesInChats[i]) {

                    val jsonObj = JSONObject(Json.decodeFromString<JsonElement>(messageJson.toString()).toString())

                    val participant = if (chat.participant1 == userLiveData.value!!.id) chat.participant2 else chat.participant1

                    val message = Message(
                        id = jsonObj.get("id").toString().toLong(),
                        body = jsonObj.get("body") as String,
                        createdAt = jsonObj.get("createdAt") as String,
                        updatedAt = jsonObj.get("updatedAt") as String,
                        createdBy = participant
                    )

                    scope.launch {
                        scope.launch { repository.addChatMessage(chat.id, message) }.join()
                        Log.d("debug", "My chat message $message in ${chat.id} chat")
                    }

                }
            }

        } }

    }

    fun sendMessage(chatId: Long, body: String) {

        if (userLiveData.value == null) return

        val jsonObj = JSONObject()
        jsonObj.put("body", body)
        Log.d("debug", "Sending chatMessage $jsonObj")
        mSocket.emit("chatMessage", chatId.toString(), jsonObj)
        mSocket.once("chatMessageId") { chatMessageIdArgs ->

            Log.d("debug", "Get chatMessageId ${chatMessageIdArgs[0]}")

            val message = Message(
                id = (chatMessageIdArgs[0] as String).toLong(),
                body = body,
                createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                updatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                createdBy = userLiveData.value!!.id
            )

            scope.launch {
                scope.launch { repository.addChatMessage(chatId, message) }.join()
                Log.d("debug", "My chat message $message in $chatId chat")
            }

        }

    }

    var connectedStatus = MutableLiveData(false)

    private val userFileName = "user.json"

    override fun onCreate() {
        super.onCreate()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val chatsNotifyChannel = "chats"
        val channel = NotificationChannel(chatsNotifyChannel,  "Chats", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "My channel description"
        channel.enableLights(true)
        channel.lightColor = Color.BLUE
        channel.enableVibration(false)
        notificationManager.createNotificationChannel(channel)

        repository = ChatRepository(LocalDatabase.getDatabase(application).chatDao())

        mSocket = IO.socket("http://plainapp.ru:3000")

        userLiveData.observe(this) { user -> scope.launch {
            if (user != null) repository.addUser(userLiveData.value!!)
        } }

        val file = File(filesDir, userFileName)
        if (file.exists() && file.isFile) {
            val data = FileInputStream(file).bufferedReader().readText()
            userLiveData.value = Json.decodeFromString<User>(data)
        }

        mSocket.on(Socket.EVENT_CONNECT) {

            Log.d("debug", "SocketIO connected")

            scope.launch { connectedStatus.postValue(true) }

            if (userLiveData.value != null) signIn {

                scope.launch { withContext(Dispatchers.Main) {
                    repository.readAllChats.observeOnce(this@SocketService) { chats -> scope.launch { updateNewMessages(chats) } }
                } }

                if (isBind) { sendOnline() }

            }

        }

        mSocket.on(Socket.EVENT_DISCONNECT) {

            Log.d("debug", "SocketIO disconnected")

            scope.launch { connectedStatus.postValue(false) }

        }

        mSocket.on(Socket.EVENT_CONNECT_ERROR) { err ->

            Log.d("debug", "SocketIO connection error: $err")

            mSocket.connect()

        }

        //MESSAGES LISTENER
        mSocket.on("chatMessage") { chatMessageArgs ->

            val chatId = when (chatMessageArgs[0].javaClass) {
                Long.Companion::class.java -> chatMessageArgs[0] as Long
                Int.Companion::class.java -> (chatMessageArgs[0] as Int).toLong()
                String.Companion::class.java -> (chatMessageArgs[0] as String).toLong()
                else -> chatMessageArgs[0].toString().toLong()
            }

            val message = Json.decodeFromString<Message>(chatMessageArgs[1].toString())

            scope.launch {
                scope.launch { repository.addChatMessage(chatId, message) }.join()
                Log.d("debug", "New chat message $message in $chatId chat")
            }

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {

                scope.launch { withContext(Dispatchers.Main) {
                    repository.readUser(message.createdBy).observeOnce(this@SocketService) { user -> scope.launch {

                        val intent = Intent(this@SocketService, ChatActivity::class.java)
                        intent.putExtra("chatId", chatId)
                        val pendingIntent = PendingIntent.getActivity(this@SocketService, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                        val notificationId = chatId.toInt()

                        val builder = Notification.Builder(
                            this@SocketService, chatsNotifyChannel)
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setContentTitle(user.name)
                            .setContentText(message.body)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)

                        notificationManager.notify(notificationId, builder.build())

                    } }
                } }

            }

        }

        //ERROR LISTENER
        mSocket.on("error") { errorArgs ->

            Log.d("debug", "SocketIO server error: ${errorArgs[0]}")

        }

        //OFFER LISTENER (CALLING WEBRTC)
        mSocket.on(SignalingCommand.OFFER.serverSignal) { offerArgs ->

            Log.d("debug", "call: get offer")

            val chatId = offerArgs[1].toString().toLong()

            val callIntent = Intent(this@SocketService, CallActivity::class.java)
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            callIntent.putExtra("offerArgs", offerArgs[0].toString())
            callIntent.putExtra("chatId", chatId)
            startActivity(callIntent)

        }

        //ONLINE USERS LISTENER
        mSocket.on("isOnline") { isOnlineArgs ->

            Log.d("debug", "get isOnline ${isOnlineArgs[0]}")

            val userId = isOnlineArgs[0].toString().toLong()
            if (!onlineUsers.contains(userId)) onlineUsers += userId

            Log.d("debug", "send isOnline to user (id = $userId)")
            mSocket.emit("isOnline", Gson().toJson(listOf(userId)))

        }
        mSocket.on("isOffline") { isOfflineArgs ->

            Log.d("debug", "get isOffline ${isOfflineArgs[0]}")

            val userId = isOfflineArgs[0].toString().toLong()
            if (onlineUsers.contains(userId)) onlineUsers.remove(userId)

        }

        mSocket.connect()

        Log.d("debug", "Service onCreate()")

    }

    fun markAsRead(chatId: Long) { mSocket.emit("markAsRead", chatId) }

    private val onlineUsers: MutableList<Long> = emptyList<Long>().toMutableList()

    fun isUserOnline(userId: Long): Boolean { return onlineUsers.contains(userId) }

    private fun sendOnline() {

        if (!mSocket.connected()) return
        if (userLiveData.value == null) return

        if (isBind) {

            scope.launch { withContext(Dispatchers.Main) {
                repository.readAllUsers.observeOnce(this@SocketService) { users ->
                    val usersToSend = emptyList<User>().toMutableList()
                    for (user in users) if (user.id != userLiveData.value!!.id) usersToSend += user
                    Log.d("debug", "send isOnline")
                    mSocket.emit("isOnline", Gson().toJson(usersToSend))
                }
            } }

        } else {

            scope.launch { withContext(Dispatchers.Main) {
                repository.readAllUsers.observeOnce(this@SocketService) { users ->
                    val usersToSend = emptyList<User>().toMutableList()
                    for (user in users) if (user.id != userLiveData.value!!.id) usersToSend += user
                    Log.d("debug", "send isOffline")
                    mSocket.emit("isOffline", Gson().toJson(usersToSend))
                }
            } }

        }

    }

    override fun onDestroy() {

        bindCount = 0
        sendOnline()

        mSocket.disconnect()
        scope.cancel()

        Log.d("debug", "Service onDestroy()")

        super.onDestroy()
    }

    private val binder: Binder = MyBinder()
    private var bindCount: Int = 0
    private val isBind get() = bindCount > 0

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        bindCount++
        if (bindCount == 1) sendOnline()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bindCount--
        if (bindCount == 0) sendOnline()
        return super.onUnbind(intent)
    }

    inner class MyBinder : Binder() {
        val service: SocketService get() = this@SocketService
    }

}