package com.example.sbreader

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

/**
 * Manages the quiz question database
 */
class QuestionDatabase(private val context: Context) {

    private val gson = Gson()
    private var questions: List<QuestionModel> = emptyList()

    init {
        loadQuestions()
    }

    /**
     * Loads questions from the raw resource file
     */
    private fun loadQuestions() {
        try {
            val jsonString = context.resources.openRawResource(R.raw.questions)
                .bufferedReader().use { it.readText() }

            val type = object : TypeToken<List<QuestionModel>>() {}.type
            questions = gson.fromJson(jsonString, type)
        } catch (e: IOException) {
            e.printStackTrace()
            // If loading fails, add some demo questions
        }
    }

    /**
     * Gets a random question from the database
     */
    fun getRandomQuestion(): QuestionModel {
        return questions.random()
    }
}