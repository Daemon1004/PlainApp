package com.example.plainapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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

    fun reg(phoneNumber: String, name: String, nickname: String, errorCallback: (text: String) -> Unit) {

        val json = JSONObject()
        json.put("phoneNumber", phoneNumber)
        json.put("name", name)
        json.put("nickname", nickname)

        mSocket.emit("createUser", json)
        Log.d("debug", "send createUser $json")
        mSocket.once("createUser") { args ->

            Log.d("debug", "get createUser ${args[0]}")
            try {

                args[0].toString().toLong()
                logIn(phoneNumber) { errorCallback("?") }

            }
            catch(e: Exception) { errorCallback(args[0].toString()) }

        }

    }

    fun logIn(phoneNumber: String, errorCallback: () -> Unit) {

        mSocket.emit("userByPN", phoneNumber)
        Log.d("debug", "send userByPN")
        mSocket.once("userByPN") { args -> scope.launch {

            Log.d("debug", "get userByPN ${args[0]}")

            if (args[0] != null) {

                val json = JSONObject(args[0].toString())

                val user = User(
                    id = json["id"].toString().toLong(),
                    nickname = json["nickname"].toString(),
                    name = json["name"].toString(),
                    bio = json["bio"].toString(),
                    birthdate = json["birthdate"].toString(),
                    phoneNumber = phoneNumber,
                    createdAt = json["createdAt"].toString()
                )

                Log.d("debug", "Get user: $user")

                scope.launch { withContext(Dispatchers.Main) {

                    userLiveData.observeOnce(this@SocketService)
                    { user -> if (user != null) signIn { updateChats() } }
                    scope.launch { userLiveData.postValue(user) }.join()
                    writeUserToFile(user)

                } }

            } else {

                errorCallback()

            }

        } }

    }

    fun logOut() {

        scope.launch { userLiveData.postValue(null) }

        val file = File(filesDir, userFileName)
        if (file.exists()) file.delete()

    }

    private fun writeUserToFile(user: User) {

        val file = File(filesDir, userFileName)
        if (!file.exists()) file.createNewFile()
        else if (file.isDirectory) {
            file.delete()
            file.createNewFile()
        }
        FileOutputStream(file).write(Json.encodeToString(user).toByteArray())

    }

    private fun signIn(callback: () -> Unit) {

        Log.d("debug", "SignIn ${userLiveData.value}")

        mSocket.emit("signin", userLiveData.value!!.id)
        mSocket.once("signin") { signInArgs ->
            val result = signInArgs[0].toString()
            if (result == "OK") {

                Log.d("debug", "SignIn OK")

                callback()

            } else {

                Log.d("debug", "SignIn error")

                signIn(callback)

            }
        }

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

                val myJson = Json { ignoreUnknownKeys = true }
                scope.launch { repository.addUsers(myJson.decodeFromString<List<User>>(getUsersArgs[0].toString())) }.join()

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
                        body = jsonObj.get("body").toString(),
                        createdAt = jsonObj.get("createdAt").toString(),
                        updatedAt = jsonObj.get("updatedAt").toString(),
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
                id = (chatMessageIdArgs[0].toString()).toLong(),
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

    private fun getString(json: JSONObject, key: String): String? {
        return try {
            json.get(key).toString()
        } catch (_: Exception) { null }
    }

    private fun updateUser(userId: Long, json: JSONObject) {

        scope.launch { withContext(Dispatchers.Main) {
            repository.readUser(userId).observeOnce(this@SocketService) { oldUser -> scope.launch {

                try {
                    if (oldUser != null) {
                        repository.addUser(
                            User(
                                id = userId,
                                nickname = getString(json, "nickname") ?: oldUser.nickname,
                                name = getString(json, "name") ?: oldUser.name,
                                bio = getString(json, "bio") ?: oldUser.bio,
                                birthdate = getString(json, "birthdate") ?: oldUser.birthdate,
                                phoneNumber = getString(json, "phoneNumber") ?: oldUser.phoneNumber,
                                createdAt = getString(json, "createdAt") ?: oldUser.createdAt
                            )
                        )
                    }
                } catch (_: Exception) {}

            } }
        } }

    }

    private fun resetUpdatedUsers() {

        mSocket.emit("updatedContacts")
        Log.d("debug", "send updatedContacts")
        mSocket.once("updatedContacts") { args ->

            Log.d("debug", "get updatedContacts ${args[0]}")

            for (jsonElement in Json.decodeFromString<List<JsonElement>>(args[0].toString())) {

                val json = JSONObject(jsonElement.toString())
                val id = json.get("id").toString().toLong()

                updateUser(id, json)

            }

        }

    }

    fun createChat(userId: Long) { mSocket.emit("createChat", userId) }

    var connectedStatus = MutableLiveData(false)

    private val userFileName = "user.json"

    override fun onCreate() {
        super.onCreate()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

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

                sendOnline(true)

                resetUpdatedUsers()

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
                String.Companion::class.java -> (chatMessageArgs[0].toString()).toLong()
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
                    repository.readUser(message.createdBy).observeOnce(this@SocketService) { user -> if (user != null) scope.launch {

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
            if (userId != userLiveData.value!!.id && !onlineUsers.contains(userId)) onlineUsers += userId

        }
        mSocket.on("isOffline") { isOfflineArgs ->

            Log.d("debug", "get isOffline ${isOfflineArgs[0]}")

            val userId = isOfflineArgs[0].toString().toLong()
            if (userId != userLiveData.value!!.id && onlineUsers.contains(userId)) onlineUsers -= userId
        }
        mSocket.on("isOnline?") { isOnlineArgs ->

            val userId = isOnlineArgs[0].toString().toLong()
            if (userId != userLiveData.value!!.id && !onlineUsers.contains(userId)) onlineUsers += userId

            Log.d("debug", "get and send isOnline? to user (id = $userId)")
            mSocket.emit("isOnline", Gson().toJson(listOf(userId)))

        }

        //USERS LISTENER
        mSocket.on("updateContact") { args ->

            Log.d("debug", "get updateContact ${args[0]} ${args[1]}")

            val id = args[0].toString().toLong()
            val json = JSONObject(args[1].toString())

            updateUser(id, json)

        }

        //CREATE CHAT LISTENER
        mSocket.on("createChat") { chatArgs ->

            Log.d("debug", "get createChat ${chatArgs[0]}")

            val chat = Json.decodeFromString<Chat>(chatArgs[0].toString())

            val participant = if (chat.participant2 == userLiveData.value!!.id) chat.participant1 else chat.participant2

            mSocket.emit("getUser", participant)
            mSocket.once("getUser") { userArgs -> scope.launch { withContext(Dispatchers.Main) {

                Log.d("debug", "createChat user ${userArgs[0]}")

                val user = Json.decodeFromString<List<User>>(userArgs[0].toString())[0]

                repository.addUser(user)
                repository.addChat(chat)

            } } }

        }

        mSocket.connect()

        Log.d("debug", "Service onCreate()")

    }

    fun newBio(bio: String, callback: (success: Boolean, text: String)->Unit) {

        scope.launch {

            val user = userLiveData.value

            if (user != null) {

                val newUser = User(
                    id = user.id,
                    nickname = user.nickname,
                    name = user.name,
                    bio = bio,
                    birthdate = user.birthdate,
                    phoneNumber = user.phoneNumber,
                    createdAt = user.createdAt
                )

                scope.launch {
                    withContext(Dispatchers.Main) {
                        repository.readAllUsers.observeOnce(this@SocketService) {
                            users -> scope.launch {

                                val userIds = emptyList<Long>().toMutableList()
                                for (u in users) userIds += u.id

                                val json = JSONObject()
                                json.put("bio", bio)
                                mSocket.emit("updateUser", json, Json.encodeToString(userIds))
                                Log.d("debug", "send updateUser")
                                mSocket.once("updateUser") {

                                    Log.d("debug", "get updateUser")

                                    userLiveData.postValue(newUser)
                                    writeUserToFile(newUser)

                                    callback(true, "")

                                }
                                mSocket.once("updateUserError") { errorArgs ->

                                    Log.d("debug", "get updateUserError ${errorArgs[0]}")

                                    if (errorArgs[0] != null)
                                        callback(false, errorArgs[0].toString())
                                    else
                                        callback(false, "Unknown error")

                                }
                            }
                        }
                    }
                }
            }
        }

    }

    fun newName(name: String, callback: (success: Boolean, text: String)->Unit) {

        scope.launch {

            val user = userLiveData.value

            if (user != null) {

                val newUser = User(
                    id = user.id,
                    nickname = user.nickname,
                    name = name,
                    bio = user.bio,
                    birthdate = user.birthdate,
                    phoneNumber = user.phoneNumber,
                    createdAt = user.createdAt
                )

                scope.launch {
                    withContext(Dispatchers.Main) {
                        repository.readAllUsers.observeOnce(this@SocketService) { users ->
                            scope.launch {

                                val userIds = emptyList<Long>().toMutableList()
                                for (u in users) userIds += u.id

                                val json = JSONObject()

                                json.put("name", name)
                                mSocket.emit("updateUser", json, Json.encodeToString(userIds))
                                Log.d("debug", "send updateUser")
                                mSocket.once("updateUser") {

                                    Log.d("debug", "get updateUser")

                                    userLiveData.postValue(newUser)
                                    writeUserToFile(newUser)

                                    callback(true, "")

                                }
                                mSocket.once("updateUserError") { errorArgs ->

                                    Log.d("debug", "get updateUserError ${errorArgs[0]}")

                                    if (errorArgs[0] != null)
                                        callback(false, errorArgs[0].toString())
                                    else
                                        callback(false, "Unknown error")

                                }
                            }
                        }
                    }
                }
            }
        }

    }

    fun searchUsers(text: String, callback: (List<User>) -> Unit) {

        mSocket.emit("userSearch", text)
        mSocket.once("userSearch") { args ->

            val users = emptyList<User>().toMutableList()

            for (element in Json.decodeFromString<List<JsonElement>>(args[0].toString())) {
                val json = JSONObject(element.toString())
                val id = json.get("id").toString().toLong()
                if (id == userLiveData.value!!.id) continue
                users += User(
                    id = id,
                    nickname = json.get("nickname").toString(),
                    name = json.get("name").toString(),
                    bio = json.get("bio").toString(),
                    birthdate = json.get("birthdate").toString(),
                    phoneNumber = json.get("phoneNumber").toString(),
                    createdAt = json.get("createdAt").toString()
                )
            }

            callback(users)

        }

    }

    fun markAsRead(chatId: Long) { mSocket.emit("markAsRead", chatId) }

    private val onlineUsers: MutableList<Long> = emptyList<Long>().toMutableList()

    fun isUserOnline(userId: Long): Boolean { return onlineUsers.contains(userId) }

    private fun sendOnline(online: Boolean) {

        if (!mSocket.connected()) return
        if (userLiveData.value == null) return

        if (online) {

            scope.launch { withContext(Dispatchers.Main) {
                repository.readAllUsers.observeOnce(this@SocketService) { users ->
                    val usersToSend = emptyList<Long>().toMutableList()
                    for (user in users) if (user.id != userLiveData.value!!.id) usersToSend += user.id
                    val json = Gson().toJson(usersToSend)
                    Log.d("debug", "send isOnline? $json")
                    mSocket.emit("isOnline?", json)
                }
            } }

        } else {

            scope.launch { withContext(Dispatchers.Main) {
                repository.readAllUsers.observeOnce(this@SocketService) { users ->
                    val usersToSend = emptyList<Long>().toMutableList()
                    for (user in users) if (user.id != userLiveData.value!!.id) usersToSend += user.id
                    val json = Gson().toJson(usersToSend)
                    Log.d("debug", "send isOffline $json")
                    mSocket.emit("isOffline", json)
                }
            } }

        }

    }

    override fun onDestroy() {

        sendOnline(false)

        mSocket.disconnect()
        scope.cancel()

        Log.d("debug", "Service onDestroy()")

        super.onDestroy()
    }

    private val binder: Binder = MyBinder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    inner class MyBinder : Binder() {
        val service: SocketService get() = this@SocketService
    }

}