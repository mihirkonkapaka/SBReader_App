package com.example.sbreader

/**
 * Utility class to validate user answers against correct answers
 */
class AnswerChecker {
    companion object {
        /**
         * Validates if the user's answer matches the correct answer based on question format
         */
        fun validateAnswer(playerAnswer: String, correctAnswer: String, format: String): Boolean {
            val sanitizedPlayerAnswer = sanitizeResponse(playerAnswer.trim()).uppercase()
            val sanitizedCorrectAnswer = sanitizeResponse(correctAnswer.trim()).uppercase()
            val cleanedCorrectAnswer = sanitizedCorrectAnswer.replace(Regex("\\(.*?\\)"), "").trim()

            return if (format == "Short Answer") {
                // For short answers, use fuzzy matching with more flexibility
                checkShortAnswer(sanitizedCorrectAnswer, sanitizedPlayerAnswer)
            } else {
                // For multiple choice, just check the letter or the answer text
                val correctLetter = cleanedCorrectAnswer.firstOrNull() ?: ""
                val correctLabel = if (cleanedCorrectAnswer.length > 3) {
                    cleanedCorrectAnswer.substring(3).trim().uppercase()
                } else {
                    ""
                }

                sanitizedPlayerAnswer == correctLetter.toString() || sanitizedPlayerAnswer == correctLabel
            }
        }

        /**
         * Checks if a short answer is correct using similarity metrics
         */
        private fun checkShortAnswer(correctAnswer: String, playerAnswer: String): Boolean {
            // Handle alternative answers in parentheses
            val alternatives = extractAlternatives(correctAnswer)

            for (alternative in alternatives) {
                // Direct match
                if (alternative.equals(playerAnswer, ignoreCase = true)) {
                    return true
                }

                // Check if player answer contains the correct answer
                if (alternative.length > 5 && playerAnswer.contains(alternative, ignoreCase = true)) {
                    return true
                }

                // For short answers, check using similarity
                if (alternative.length > 1 && similarity(alternative, playerAnswer) > 0.8) {
                    return true
                }
            }

            return false
        }

        /**
         * Extract main answer and alternatives from a correct answer string
         */
        private fun extractAlternatives(answer: String): List<String> {
            val result = mutableListOf<String>()

            // Add the main answer without any parenthetical content
            result.add(answer.replace(Regex("\\(.*?\\)"), "").trim())

            // Extract alternatives from parentheses
            val parentheticalPattern = Regex("\\((.*?)\\)")
            val matches = parentheticalPattern.findAll(answer)

            for (match in matches) {
                result.add(match.groupValues[1].trim())
            }

            return result
        }

        /**
         * Calculates string similarity using Levenshtein distance
         */
        private fun similarity(s1: String, s2: String): Double {
            val longer = if (s1.length > s2.length) s1 else s2
            val shorter = if (s1.length > s2.length) s2 else s1
            val longerLength = longer.length

            if (longerLength == 0) return 1.0

            return (longerLength - editDistance(longer, shorter)) / longerLength.toDouble()
        }

        /**
         * Calculates Levenshtein edit distance between two strings
         */
        private fun editDistance(s1: String, s2: String): Int {
            val s1Lower = s1.lowercase()
            val s2Lower = s2.lowercase()

            val costs = IntArray(s2Lower.length + 1)

            for (i in 0..s1Lower.length) {
                var lastValue = i
                for (j in 0..s2Lower.length) {
                    if (i == 0) {
                        costs[j] = j
                    } else if (j > 0) {
                        var newValue = costs[j - 1]
                        if (s1Lower[i - 1] != s2Lower[j - 1]) {
                            newValue = minOf(minOf(newValue, lastValue), costs[j]) + 1
                        }
                        costs[j - 1] = lastValue
                        lastValue = newValue
                    }
                }
                if (i > 0) {
                    costs[s2Lower.length] = lastValue
                }
            }

            return costs[s2Lower.length]
        }

        /**
         * Sanitizes a response by removing special characters
         */
        private fun sanitizeResponse(response: String): String {
            return response.replace("`", "")
        }
    }
}