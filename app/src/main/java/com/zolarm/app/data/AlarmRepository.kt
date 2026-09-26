package com.zolarm.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AlarmRepository(context: Context) {
    private val prefs = context.getSharedPreferences("zolarm_store", Context.MODE_PRIVATE)

    fun load(): MutableList<Alarm> {
        val raw = prefs.getString("alarms", null) ?: return defaultSeed()
        val out = mutableListOf<Alarm>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Alarm(
                    id = o.getInt("id"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    label = o.getString("label"),
                    challenge = ChallengeType.valueOf(o.getString("challenge")),
                    stepTarget = o.optInt("stepTarget", 10),
                    enabled = o.getBoolean("enabled")
                )
            )
        }
        return out
    }

    fun save(alarms: List<Alarm>) {
        val arr = JSONArray()
        alarms.forEach { a ->
            arr.put(
                JSONObject()
                    .put("id", a.id).put("hour", a.hour).put("minute", a.minute)
                    .put("label", a.label).put("challenge", a.challenge.name)
                    .put("stepTarget", a.stepTarget).put("enabled", a.enabled)
            )
        }
        prefs.edit().putString("alarms", arr.toString()).apply()
    }

    fun nextId(): Int {
        val id = prefs.getInt("next_id", 1000) + 1
        prefs.edit().putInt("next_id", id).apply()
        return id
    }

    private fun defaultSeed() = mutableListOf(
        Alarm(1001, 7, 0, "استيقاظ — انهض", ChallengeType.STEPS, 10, true),
        Alarm(1002, 18, 30, "وقت التمرين", ChallengeType.CAMERA, 10, false)
    )
}
