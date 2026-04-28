package com.onmi.qing.data

import android.util.Log
import com.google.gson.Gson

data class Recommendation(
    val type: String,
    val title: String,
    val description: String,
    val action: String
)

data class CrisisIntervention(
    val title: String,
    val description: String,
    val phone: String,
    val action: String
)

fun parseRecommendation(content: String): Recommendation? {
    try {
        // 格式1: JSON对象包装 {"recommendation":{...}}
        if (content.contains("\"recommendation\"")) {
            val json = content.trim()
            val wrapper = Gson().fromJson(json, RecommendationWrapper::class.java)
            if (wrapper?.recommendation != null) {
                return wrapper.recommendation
            }
        }

        // 格式2: [RECOMMENDATION:{...}] (Function Calling返回格式)
        val startMarker = "[RECOMMENDATION:"
        val endMarker = "]"
        val startIndex = content.indexOf(startMarker)
        if (startIndex != -1) {
            val jsonStart = content.indexOf("{", startIndex)
            var searchFrom = jsonStart
            var depth = 0
            // Find matching closing brace
            while (searchFrom < content.length) {
                when (content[searchFrom]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            val jsonEnd = searchFrom + 1
                            val json = content.substring(jsonStart, jsonEnd)
                            return Gson().fromJson(json, Recommendation::class.java)
                        }
                    }
                }
                searchFrom++
            }
        }

        // 格式3: tool_use input_json 格式 {"type":"input_json","snapshot":{...}}
        if (content.contains("\"snapshot\"")) {
            val inputJson = Gson().fromJson(content, InputJsonWrapper::class.java)
            if (inputJson?.snapshot != null) {
                return inputJson.snapshot
            }
        }
    } catch (e: Exception) {
        Log.e("Recommendation", "parseRecommendation failed: ${e.message}")
    }
    return null
}

fun parseCrisisIntervention(content: String): CrisisIntervention? {
    try {
        // 格式1: [CRISIS:{...}] 格式
        val startMarker = "[CRISIS:"
        val startIndex = content.indexOf(startMarker)
        if (startIndex != -1) {
            val jsonStart = content.indexOf("{", startIndex)
            var depth = 0
            var jsonEnd = -1
            for (i in jsonStart until content.length) {
                when (content[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            jsonEnd = i + 1
                            break
                        }
                    }
                }
            }
            if (jsonEnd != -1) {
                val json = content.substring(jsonStart, jsonEnd)
                return Gson().fromJson(json, CrisisIntervention::class.java)
            }
        }

        // 格式2: tool_use input_json 格式 {"type":"input_json","snapshot":{...}}
        if (content.contains("\"snapshot\"")) {
            val inputJson = Gson().fromJson(content, CrisisInputJsonWrapper::class.java)
            if (inputJson?.snapshot != null) {
                return inputJson.snapshot
            }
        }
    } catch (e: Exception) {
        Log.e("CrisisIntervention", "parseCrisisIntervention failed: ${e.message}")
    }
    return null
}

private class RecommendationWrapper {
    var recommendation: Recommendation? = null
}

private class InputJsonWrapper {
    var snapshot: Recommendation? = null
}

private class CrisisInputJsonWrapper {
    var snapshot: CrisisIntervention? = null
}
