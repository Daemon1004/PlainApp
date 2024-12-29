package com.example.plainapp

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.plainapp.data.Chat
import com.example.plainapp.data.ChatRepository
import com.example.plainapp.data.LocalDatabase
import com.example.plainapp.data.Message
import com.example.plainapp.data.User
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("NAME_SHADOWING")
class SocketService : Service() {

    private val scope = CoroutineScope( EmptyCoroutineContext )

    private lateinit var mSocket: Socket

    private lateinit var repository: ChatRepository

    /*
    fun listenOnce(event: String, success: Function<Array<Any>>, error: Function<String>) {

        mSocket.once(event) { args ->

            when (args[0] as String) {
                "OK" -> {}
                "ERROR" -> {}
                else -> {}
            }

        }

    }
    */

    var userLiveData: MutableLiveData<User?> = MutableLiveData<User?>()

    fun logIn(phoneNumber: String) {

        mSocket.emit("userByPN", phoneNumber)
        mSocket.once("userByPN") { args -> scope.launch {

            scope.launch { userLiveData.postValue(Json.decodeFromString<User>(args[0].toString())) }.join()

            Log.d("debug", "Get user: ${userLiveData.value}")

            scope.launch {

                scope.launch { repository.addUser(userLiveData.value!!) }.join()

                signIn()

                init()

            }

        } }
    }

    private fun signIn() {

        mSocket.emit("signin", userLiveData.value!!.id)

    }

    private fun init() {

        updateChats()

    }

    private fun updateChats() {

        mSocket.emit("myChats")
        mSocket.once("myChats") { args ->

            val chats = Json.decodeFromString<List<Chat>>(args[0].toString())

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
            mSocket.once("getUsers") { args -> scope.launch {

                scope.launch { repository.addUsers(Json.decodeFromString<List<User>>(args[0].toString())) }.join()
                scope.launch { repository.deleteAllChats() }.join()
                scope.launch { repository.writeChats(chats) }.join()

                mSocket.on("chatMessage") { args ->

                    val chatId = when (args[0].javaClass) {
                        Long.Companion::class.java -> args[0] as Long
                        Int.Companion::class.java -> (args[0] as Int).toLong()
                        String.Companion::class.java -> (args[0] as String).toLong()
                        else -> args[0].toString().toLong()
                    }

                    //Log.d("debug", "newChatMessage arg0 - ${args[0]} (${args[0].javaClass})")

                    val message = Json.decodeFromString<Message>(args[1].toString())

                    scope.launch {
                        scope.launch { repository.addChatMessage(chatId, message) }.join()
                        Log.d("debug", "newChatMessage $message in $chatId chat")
                    }

                }

            } }

        }

    }

    fun sendMessage(chatId: Long, body: String) {

        if (userLiveData.value == null) return

        val jsonObj = JSONObject()
        jsonObj.put("body", body)
        Log.d("debug", "sending chatMessage $jsonObj")
        mSocket.emit("chatMessage", chatId.toString(), jsonObj)

        mSocket.once("chatMessageId") { args ->

            Log.d("debug", "get chatMessageId ${args[0]}")

            val message = Message(
                id = (args[0] as String).toLong(),
                body = body,
                createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                updatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                createdBy = userLiveData.value!!.id
            )
            scope.launch {
                scope.launch { repository.addChatMessage(chatId, message) }.join()
                Log.d("debug", "newChatMessage $message in $chatId chat")
            }

        }

    }

    var connected = false

    override fun onCreate() {
        super.onCreate()

        repository = ChatRepository(LocalDatabase.getDatabase(application).chatDao())

        mSocket = IO.socket("http://plainapp.ru:3000")

        mSocket.on(Socket.EVENT_CONNECT) {

            Log.d("debug", "SocketIO. Connected")

            connected = true

            if (userLiveData.value != null) signIn()

        }

        mSocket.on(Socket.EVENT_DISCONNECT) {

            Log.d("debug", "SocketIO. Disconnected")

            connected = false

        }

        mSocket.on(Socket.EVENT_CONNECT_ERROR) { err ->

            Log.d("debug", "SocketIO. Connection error: $err")

            mSocket.connect()

        }

        mSocket.connect()

        Log.d("debug", "Service onCreate()")

    }

    override fun onDestroy() {

        mSocket.disconnect()
        scope.cancel()

        Log.d("debug", "Service onDestroy()")

        super.onDestroy()
    }

    private val binder: Binder = MyBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class MyBinder : Binder() {
        val service: SocketService get() = this@SocketService
    }

}