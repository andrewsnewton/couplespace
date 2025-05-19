package com.newton.couplespace.services

import android.util.Log
import com.newton.couplespace.models.QuestionTopic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random

/**
 * Service for providing relationship questions without external APIs
 */
class QuestionService {
    private val TAG = "QuestionService"
    private val random = Random()
    
    // Comprehensive question bank organized by topic
    private val questionBank = mapOf(
        QuestionTopic.NAUGHTY to listOf(
            "What's your favorite part of my body?",
            "What's a fantasy you've never told me about?",
            "What's something you'd like to try in the bedroom?",
            "Where's the most adventurous place you'd like to be intimate?",
            "What outfit would you love to see me wear?",
            "If you could only touch one part of my body for the rest of our lives, what would it be?",
            "What's something you've always wanted to try but were too shy to ask?",
            "What's the sexiest dream you've had about me?",
            "What's your favorite memory of us being intimate?",
            "If we had 24 hours alone with no interruptions, what would you want to do?",
            "What's one thing I do that turns you on the most?",
            "What's a secret spot on your body that you wish I'd pay more attention to?",
            "What's something new you'd like to explore together?",
            "If you could recreate any intimate moment we've had, which would it be?",
            "What's something that would surprise me about your desires?",
            "What's a fantasy location where you'd like us to be intimate?",
            "What's something I've done that you wish I'd do more often?",
            "If you could design the perfect romantic evening, what would it include?",
            "What's something you find attractive about me that I might not know?",
            "What's the most memorable compliment I've given you?"
        ),
        
        QuestionTopic.FUNNY to listOf(
            "If we were a superhero duo, what would our powers be?",
            "What's the weirdest thing you've caught me doing?",
            "If we switched bodies for a day, what would you do first?",
            "What's your best impression of me?",
            "If we were in a zombie apocalypse, what role would each of us play?",
            "What's the funniest thing that's happened to us as a couple?",
            "If you could replace me with any celebrity for a day, who would it be and why?",
            "What's the most embarrassing thing I've done in front of you?",
            "If our relationship was a movie, what genre would it be and who would play us?",
            "What's a weird habit of mine that you secretly find endearing?",
            "If we had a band, what would it be called and what kind of music would we play?",
            "What's the silliest argument we've ever had?",
            "If we were animals, what would we be and why?",
            "What's something ridiculous you'd like us to do together someday?",
            "If you had to describe me as a food, what would I be?",
            "What's the weirdest thing you've seen in my search history?",
            "If we were stranded on a desert island, which one of us would survive longer?",
            "What's a nickname you've secretly wanted to call me but never have?",
            "If our relationship had a theme song, what would it be?",
            "What's the most bizarre thought you've had about our future together?"
        ),
        
        QuestionTopic.EMOTIONAL to listOf(
            "When did you first realize you were falling for me?",
            "What's one thing I do that always makes you feel loved?",
            "What's a moment between us that you'll never forget?",
            "How have I helped you grow as a person?",
            "What's one thing you hope we'll accomplish together in the future?",
            "What's something about our relationship that you're most proud of?",
            "When do you feel closest to me?",
            "What's a time when I really came through for you?",
            "What's something I've taught you about yourself?",
            "What's your favorite quality about me that others might not notice?",
            "What's something you'd like me to know about how you feel loved?",
            "What's a challenge we've overcome together that made us stronger?",
            "What's a small moment between us that meant a lot to you?",
            "How has your definition of love changed since we've been together?",
            "What's something you admire about how I handle difficulties?",
            "What's a way I've supported you that you really appreciated?",
            "What's something about our relationship that surprised you?",
            "What's a value or belief we share that's important to you?",
            "What's something you're looking forward to experiencing with me?",
            "What's a time when you felt most understood by me?",
            "What's something about me that inspires you?",
            "What's a dream you have for us that you haven't shared yet?",
            "What's something I do that makes you feel secure in our relationship?",
            "What's a quality in me that complements you well?",
            "What's a memory of us that always makes you smile?"
        )
    )
    
    /**
     * Get a random question for the specified topic
     * Uses a large built-in question bank for immediate results
     */
    suspend fun getRandomQuestion(topic: QuestionTopic): String = withContext(Dispatchers.Default) {
        // Log the request
        Log.d(TAG, "Getting random question for topic: ${topic.name}")
        
        // Get questions for the specified topic
        val questions = questionBank[topic] ?: emptyList()
        
        if (questions.isEmpty()) {
            Log.w(TAG, "No questions found for topic: ${topic.name}")
            return@withContext "What's something you'd like to talk about?"
        }
        
        // Get a truly random question
        val randomIndex = random.nextInt(questions.size)
        val question = questions[randomIndex]
        
        Log.d(TAG, "Selected question: $question")
        return@withContext question
    }
}
