package com.example.sbreader

/**
 * Data class that represents a quiz question with both tossup and bonus components
 */
data class QuestionModel(
    val tossup_question: String,
    val tossup_answer: String,
    val tossup_format: String,
    val bonus_question: String,
    val bonus_answer: String,
    val bonus_format: String,
    val category: String,
    val api_url: String,
    val source: String
)
/**
 * {
 *   "questions": [{
 *       "api_url": "API URL of question",
 *       "bonus_answer": "BONUS ANSWER",
 *       "bonus_format": "Short Answer/Bonus",
 *       "bonus_question": "Bonus question text"
 *       "category": "CATEGORY",
 *       "id": question number,
 *       "source": "Source of question",
 *       "tossup_answer": "TOSSUP ANSWER",
 *       "tossup_format": "Short Answer/Bonus",
 *       "tossup_question": "Tossup question text"
 *       "uri": "permalink to question"
 *     }, etc. ]
 * }
 *
 */

/**
 * Enum representing the current question state
 */
enum class QuestionState {
    IDLE,
    READING_TOSSUP,
    BUZZED_TOSSUP,
    ANSWERING_TOSSUP,
    READING_BONUS,
    BUZZED_BONUS,
    ANSWERING_BONUS,
    COMPLETED
}