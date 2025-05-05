package com.example.sbreader

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Html
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.ScrollView

class MainActivity : AppCompatActivity() {
    // UI Components
    private lateinit var questionTypeText: TextView
    private lateinit var questionText: TextView
    private lateinit var answerResult: TextView
    private lateinit var correctionButton: Button
    private lateinit var answerInputSection: LinearLayout
    private lateinit var answerInput: EditText
    private lateinit var submitAnswerButton: Button
    private lateinit var timerProgressBar: ProgressBar
    private lateinit var timerText: TextView
    private lateinit var scoreDisplay: TextView
    private lateinit var contentScrollView: ScrollView
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var skipButton: Button
    private lateinit var resetStatsButton: Button
    private lateinit var buzzButton: Button

    // Game state
    private var currentQuestion: QuestionModel? = null
    private var questionDatabase: QuestionDatabase? = null
    private var currentWordIndex = 0
    private var questionWords = listOf<String>()
    private var hasBuzzed = false
    private var answeringBonus = false
    private var score = 0
    private var readyForNext = false
    private var wasReading = false
    private var isPaused = false
    private var timeLeft = 0f

    // Timers
    private var readingTimer: CountDownTimer? = null
    private var buzzTimer: CountDownTimer? = null
    private var answerTimer: CountDownTimer? = null

    // Constants
    private val TOSSUP_TIME_LIMIT = 20 // seconds
    private val BONUS_TIME_LIMIT = 40 // seconds
    private val ANSWER_TIME_LIMIT = 5 // seconds
    private val WORD_DELAY = 300L // milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        initializeViews()

        // Set up listeners
        setupListeners()

        // Initialize the question database
        questionDatabase = QuestionDatabase(this)
    }

    private fun initializeViews() {
        questionTypeText = findViewById(R.id.questionType)
        questionText = findViewById(R.id.questionText)
        answerResult = findViewById(R.id.answerResult)
        correctionButton = findViewById(R.id.correctionButton)
        answerInputSection = findViewById(R.id.answerInputSection)
        answerInput = findViewById(R.id.answerInput)
        submitAnswerButton = findViewById(R.id.submitAnswerButton)
        timerProgressBar = findViewById(R.id.timerProgressBar)
        timerText = findViewById(R.id.timerText)
        scoreDisplay = findViewById(R.id.scoreDisplay)
        startButton = findViewById(R.id.startButton)
        pauseButton = findViewById(R.id.pauseButton)
        skipButton = findViewById(R.id.skipButton)
        resetStatsButton = findViewById(R.id.resetStatsButton)
        buzzButton = findViewById(R.id.buzzButton)
        contentScrollView = findViewById(R.id.contentScrollView)
    }

    private fun setupListeners() {
        // Start button initiates a new question
        startButton.setOnClickListener {
            loadAndStartQuestion()
        }

        // Pause button pauses/resumes reading
        pauseButton.setOnClickListener {
            pauseReading()
        }

        // Skip button skips current question or moves to next
        skipButton.setOnClickListener {
            handleSkipOrNext()
        }

        // Buzz button to answer
        buzzButton.setOnClickListener {
            buzz()
        }

        // Reset stats button
        resetStatsButton.setOnClickListener {
            resetStats()
        }

        // Submit answer button
        submitAnswerButton.setOnClickListener {
            submitAnswer()
        }

        // Handle "Done" button on keyboard
        answerInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                submitAnswer()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun loadAndStartQuestion() {
        // Reset state
        pauseButton.text = getString(R.string.pause)
        readyForNext = false
        skipButton.text = getString(R.string.skip)

        // Clear all timers
        clearTimers()

        // Get a random question
        currentQuestion = questionDatabase?.getRandomQuestion()
        currentQuestion?.let { question ->
            questionTypeText.text = "TOSSUP ${question.tossup_format} ${question.category}"
            startTossup()
        }
    }

    private fun startTossup() {
        // Reset states
        answeringBonus = false
        hasBuzzed = false
        isPaused = false
        answerResult.visibility = View.GONE
        correctionButton.visibility = View.GONE
        answerInputSection.visibility = View.GONE
        answerInput.text.clear()
        buzzButton.isEnabled = true

        // Reset timer UI
        updateTimerBar(0f, null)

        // Split question text into words
        currentQuestion?.let { question ->
            questionWords = question.tossup_question.split(" ")
            currentWordIndex = 0
            questionText.text = ""

            // Clear previous timers
            clearTimers()

            // Start reading the question
            startReadingWords()
        }
    }

    private fun startReadingWords() {
        if (currentWordIndex < questionWords.size) {
            val wordDelay = WORD_DELAY

            readingTimer = object : CountDownTimer(wordDelay, wordDelay) {
                override fun onTick(millisUntilFinished: Long) {
                    // Nothing needed here
                }

                override fun onFinish() {
                    if (!isPaused && currentWordIndex < questionWords.size) {
                        questionText.append("${questionWords[currentWordIndex]} ")
                        contentScrollView.post {
                            contentScrollView.fullScroll(View.FOCUS_DOWN)
                        }
                        currentWordIndex++

                        // Continue with next word
                        if (currentWordIndex < questionWords.size) {
                            startReadingWords()
                        } else {
                            // Finished reading the question
                            startBuzzTimer()
                        }
                    }
                }
            }.start()
        } else {
            // If all words are read, start the buzz timer
            startBuzzTimer()
        }
    }

    private fun startBuzzTimer(remainingTime: Float? = null) {
        val totalTime = if (answeringBonus) BONUS_TIME_LIMIT else TOSSUP_TIME_LIMIT
        timeLeft = remainingTime ?: totalTime.toFloat()

        val timerLength = (timeLeft * 1000).toLong()

        updateTimerBar(timeLeft / totalTime, timeLeft.toInt())

        // Cancel previous timer if exists
        buzzTimer?.cancel()

        buzzTimer = object : CountDownTimer(timerLength, 100) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isPaused) {
                    timeLeft = millisUntilFinished / 1000f
                    updateTimerBar(timeLeft / totalTime, timeLeft.toInt() + 1)
                }
            }

            override fun onFinish() {
                if (!isPaused) {
                    timeLeft = 0f
                    updateTimerBar(0f, 0)
                    handleSkipOrNext()
                }
            }
        }.start()
    }

    private fun startAnswerTimer() {
        timeLeft = ANSWER_TIME_LIMIT.toFloat()

        // Update UI
        updateTimerBar(1f, ANSWER_TIME_LIMIT)

        // Cancel previous timer if exists
        answerTimer?.cancel()

        answerTimer = object : CountDownTimer(ANSWER_TIME_LIMIT * 1000L, 100) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isPaused) {
                    timeLeft = millisUntilFinished / 1000f
                    updateTimerBar(timeLeft / ANSWER_TIME_LIMIT, timeLeft.toInt() + 1)
                }
            }

            override fun onFinish() {
                if (!isPaused && !readyForNext) {
                    submitAnswer()
                }
            }
        }.start()
    }

    private fun buzz() {
        if (hasBuzzed) return

        hasBuzzed = true
        answerInputSection.visibility = View.VISIBLE
        answerInput.requestFocus()

        answerInput.showKeyboard()

        // Stop reading and buzz timers
        readingTimer?.cancel()
        buzzTimer?.cancel()

        // Disable buzz button
        buzzButton.isEnabled = false

        // Hide answer result
        answerResult.visibility = View.GONE

        // Start answer timer
        startAnswerTimer()
    }

    private fun submitAnswer() {
        // Hide answer input
        answerInputSection.visibility = View.GONE

        // Cancel timer
        answerTimer?.cancel()
        updateTimerBar(0f, null)

        val playerAnswer = answerInput.text.toString().trim()
        val correctAnswer = if (answeringBonus) {
            currentQuestion?.bonus_answer ?: ""
        } else {
            currentQuestion?.tossup_answer ?: ""
        }

        val format = if (answeringBonus) {
            currentQuestion?.bonus_format ?: "Short Answer"
        } else {
            currentQuestion?.tossup_format ?: "Short Answer"
        }

        val isCorrect = AnswerChecker.validateAnswer(playerAnswer, correctAnswer, format)

        // Clear input field
        answerInput.text.clear()

        answerInput.hideKeyboard()

        // Show result
        showAnswerResult(correctAnswer, isCorrect)

        // Update score
        val finishedTossup = (currentWordIndex == questionWords.size)
        if (isCorrect) {
            score += if (answeringBonus) 10 else 4
        } else {
            if (!answeringBonus && !finishedTossup) score -= 4
        }

        // Show correction button
        correctionButton.text = if (isCorrect) {
            getString(R.string.i_was_wrong)
        } else {
            getString(R.string.i_was_correct)
        }
        correctionButton.setBackgroundColor(
            ContextCompat.getColor(this, if (isCorrect) R.color.red else R.color.green)
        )
        correctionButton.setOnClickListener {
            correctScore(isCorrect)
        }
        correctionButton.visibility = View.VISIBLE

        // Mark question as ready for next
        readyForNext = true
        skipButton.text = getString(R.string.next)

        // If tossup was answered correctly, proceed to bonus
        if (!answeringBonus && isCorrect) {
            answeringBonus = true
            val delay = minOf(5000, (500 + 75 * (currentQuestion?.tossup_answer?.split(" ")?.size ?: 0))).toLong()
            correctionButton.postDelayed({ startBonus() }, delay)
        }

        updateScore()
    }

    private fun startBonus() {
        // Reset UI
        updateTimerBar(0f, null)
        hasBuzzed = false
        answeringBonus = true
        buzzButton.isEnabled = true
        answerInputSection.visibility = View.GONE
        answerInput.text.clear()

        // Update question type
        currentQuestion?.let { question ->
            questionTypeText.text = "BONUS ${question.bonus_format} ${question.category}"
        }

        // Hide previous results
        answerResult.visibility = View.GONE
        correctionButton.visibility = View.GONE

        // Split bonus question text into words
        currentQuestion?.let { question ->
            questionWords = question.bonus_question.split(" ")
            currentWordIndex = 0
            questionText.text = ""

            // Clear previous timers
            clearTimers()

            // Start reading the bonus question
            startReadingWords()
        }
    }

    private fun clearTimers() {
        readingTimer?.cancel()
        buzzTimer?.cancel()
        answerTimer?.cancel()

        // Reset timer display
        updateTimerBar(0f, null)
    }

    private fun pauseReading() {
        isPaused = !isPaused
        pauseButton.text = getString(if (isPaused) R.string.resume else R.string.pause)

        if (!isPaused && wasReading && !hasBuzzed) {
            // Resume reading
            startReadingWords()
        }
    }

    private fun handleSkipOrNext() {
        // Clear all timers
        clearTimers()

        // Enable buzz button for next question
        buzzButton.isEnabled = true

        // Display full question
        currentQuestion?.let { question ->
            questionText.text = if (answeringBonus) question.bonus_question else question.tossup_question
        }

        if (!readyForNext) {
            // Show correct answer if skipping
            val correctAnswer = if (answeringBonus) {
                currentQuestion?.bonus_answer ?: ""
            } else {
                currentQuestion?.tossup_answer ?: ""
            }
            showAnswerResult(correctAnswer, false)
        }

        // Reset state for next question
        readyForNext = false

        // Load new question
        loadAndStartQuestion()
    }

    private fun correctScore(wasCorrect: Boolean) {
        if (wasCorrect) {
            // Deduct points if initially correct but actually wrong
            score -= if (answeringBonus) 10 else 8
        } else {
            // Add points if initially wrong but actually correct
            score += if (answeringBonus) 10 else 8

            // If it was a tossup that should have been correct, proceed to bonus
            if (!answeringBonus) {
                correctionButton.postDelayed({ startBonus() }, 1000)
            }
        }
        updateScore()
        correctionButton.visibility = View.GONE
    }

    private fun showAnswerResult(correctAnswer: String, isCorrect: Boolean) {
        // Set the text and make visible
        answerResult.text = getString(R.string.correct_answer, correctAnswer)
        answerResult.setTextColor(
            ContextCompat.getColor(this, if (isCorrect) R.color.correct else R.color.incorrect)
        )
        answerResult.visibility = View.VISIBLE

        // Show full question text
        currentQuestion?.let { question ->
            questionText.text = if (answeringBonus) question.bonus_question else question.tossup_question
        }
    }

    private fun updateTimerBar(progress: Float, seconds: Int?) {
        // Update the progress bar
        val progressPercentage = (progress * 100).toInt()

        // Use an animator for smooth transitions
        ValueAnimator.ofInt(timerProgressBar.progress, progressPercentage).apply {
            duration = 100
            addUpdateListener { animator ->
                timerProgressBar.progress = animator.animatedValue as Int
            }
            start()
        }

        // Update the timer text if seconds are provided
        timerText.text = seconds?.toString() ?: ""
    }

    private fun resetStats() {
        score = 0
        updateScore()
    }

    private fun updateScore() {
        scoreDisplay.text = getString(R.string.score_display, score)
    }

    // Handle volume buttons for buzz
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (buzzButton.isEnabled) {
                    buzz()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause timers when app goes to background
        if (!isPaused) {
            pauseReading()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up timers
        clearTimers()
    }

    private fun View.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

}