package app.hundred

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

const val GOAL = 100

data class Skill(val id: Long, val name: String, val attempts: Int = 0) {
    val remaining get() = (GOAL - attempts).coerceAtLeast(0)
    val canQuit get() = attempts >= GOAL
}

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("skills", Context.MODE_PRIVATE)

    fun load(): List<Skill> = runCatching {
        val arr = JSONArray(prefs.getString("list", "[]"))
        List(arr.length()) {
            val o = arr.getJSONObject(it)
            Skill(o.getLong("id"), o.getString("name"), o.getInt("attempts"))
        }
    }.getOrDefault(emptyList())

    fun save(skills: List<Skill>) {
        val arr = JSONArray()
        skills.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("attempts", it.attempts))
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }
}
