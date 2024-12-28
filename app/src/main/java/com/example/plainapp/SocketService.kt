package com.example.plainapp

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
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

    var user: User? = null

    fun logIn(phoneNumber: String) {

        mSocket.emit("userByPN", phoneNumber)
        mSocket.once("userByPN") { args ->

            user = Json.decodeFromString<User>(args[0].toString())

            Log.d("debug", "Get user: $user")

            scope.launch {

                scope.launch { repository.addUser(user!!) }.join()

            }

            mSocket.emit("signin", user!!.id)
            mSocket.emit("myChats")
            mSocket.once("myChats") { args ->

                val chats = Json.decodeFromString<List<Chat>>(args[0].toString())

                val userIds = mutableListOf<Long>()
                for (chat in chats) {
                    when (user!!.id) {
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

                        val chatId = (args[0] as String).toLong()
                        val message = Json.decodeFromString<Message>(args[1].toString())

                        scope.launch { repository.addChatMessage(chatId, message) }

                    }

                } }

                scope.launch {

                    /*
                    mSocket.on("createChat") { args ->

                        val chat = Json.decodeFromString<Chat>(args[0].toString())
                        scope.launch { repository.addChat(chat) }

                    }

                    mSocket.on("deleteChat") { args ->

                        val chatId = Json.decodeFromString<Long>(args[0].toString())
                        scope.launch { repository.deleteChat(chatId) }

                    }

                     */

                }

            }

        }

    }

    fun sendMessage(chatId: Long, body: String) {

        if (user == null) return

        //FIXME
        mSocket.emit("chatMessage", chatId, JSONObject("{\"body\":\"$body\"}"))

        mSocket.once("chatMessageId") { args ->

            val message = Message(
                id = (args[0] as String).toLong(),
                body = body,
                createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                updatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                createdBy = user!!.id
            )
            scope.launch { repository.addChatMessage(chatId, message) }

        }

    }

    override fun onCreate() {
        super.onCreate()

        repository = ChatRepository(LocalDatabase.getDatabase(application).chatDao())

        mSocket = IO.socket("http://plainapp.ru:3000")
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