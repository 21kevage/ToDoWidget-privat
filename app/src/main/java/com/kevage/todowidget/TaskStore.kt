package com.kevage.todowidget

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

internal const val MAX_TITLE_LENGTH = 200

internal data class Task(
    val id: String,
    val title: String,
    val completed: Boolean = false,
)

internal class TaskStore private constructor(context: Context) {
    private val preferences = context.getSharedPreferences("my_tasks", Context.MODE_PRIVATE)
    private val lock = Any()
    private val savedTasks = MutableStateFlow(read())
    val tasks = savedTasks.asStateFlow()

    suspend fun add(title: String) = mutate { current ->
        current + Task(UUID.randomUUID().toString(), title)
    }

    suspend fun rename(id: String, title: String) = mutate { current ->
        current.map { if (it.id == id) it.copy(title = title) else it }
    }

    suspend fun setCompleted(id: String, completed: Boolean) = mutate { current ->
        current.map { if (it.id == id) it.copy(completed = completed) else it }
    }

    suspend fun delete(id: String) = mutate { current ->
        current.filterNot { it.id == id }
    }

    private suspend fun mutate(transform: (List<Task>) -> List<Task>) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val current = savedTasks.value
                val next = transform(current)
                if (next == current) return@synchronized
                val json = JSONArray()
                next.forEach { task ->
                    json.put(
                        JSONObject()
                            .put("id", task.id)
                            .put("title", task.title)
                            .put("completed", task.completed),
                    )
                }
                if (!preferences.edit().putString("tasks", json.toString()).commit()) {
                    throw IOException("Could not save tasks")
                }
                savedTasks.value = next
            }
        }

    private fun read(): List<Task> {
        val json = JSONArray(preferences.getString("tasks", "[]"))
        return List(json.length()) { index ->
            val item = json.getJSONObject(index)
            Task(item.getString("id"), item.getString("title"), item.getBoolean("completed"))
        }
    }

    companion object {
        private var instance: TaskStore? = null

        @Synchronized
        fun get(context: Context): TaskStore =
            instance ?: TaskStore(context.applicationContext).also { instance = it }
    }
}
