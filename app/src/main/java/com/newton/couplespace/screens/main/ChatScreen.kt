package com.newton.couplespace.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.newton.couplespace.models.CoupleQuestion
import com.newton.couplespace.models.Message
import com.newton.couplespace.models.QuestionTopic
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: androidx.navigation.NavController) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var partnerUserId by remember { mutableStateOf<String?>(null) }
    var partnerName by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var questions by remember { mutableStateOf<List<CoupleQuestion>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Get partner ID and name
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            FirebaseFirestore.getInstance().collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    partnerUserId = document.getString("partnerId")
                    
                    // Get partner name if partner exists
                    partnerUserId?.let { partnerId ->
                        FirebaseFirestore.getInstance().collection("users")
                            .document(partnerId)
                            .get()
                            .addOnSuccessListener { partnerDoc ->
                                partnerName = partnerDoc.getString("name")
                                isLoading = false
                            }
                    } ?: run {
                        isLoading = false
                    }
                }
        }
    }
    
    // Listen for messages in real-time
    var messagesListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var questionsListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    
    DisposableEffect(currentUserId, partnerUserId) {
        if (currentUserId.isNotBlank() && partnerUserId != null) {
            // Listen for messages
            messagesListener = FirebaseFirestore.getInstance().collection("messages")
                .whereIn("senderId", listOf(currentUserId, partnerUserId))
                .whereIn("receiverId", listOf(currentUserId, partnerUserId))
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }
                    
                    snapshot?.let { docs ->
                        messages = docs.mapNotNull { doc ->
                            doc.toObject(Message::class.java)
                        }
                        
                        // Scroll to bottom on new messages
                        if (messages.isNotEmpty()) {
                            coroutineScope.launch {
                                listState.scrollToItem(messages.size - 1)
                            }
                        }
                    }
                }
            
            // Listen for couple questions
            questionsListener = FirebaseFirestore.getInstance().collection("coupleQuestions")
                .whereArrayContains("userIds", listOf(currentUserId, partnerUserId))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }
                    
                    snapshot?.let { docs ->
                        questions = docs.mapNotNull { doc ->
                            doc.toObject(CoupleQuestion::class.java)
                        }
                    }
                }
        }
        
        onDispose {
            messagesListener?.remove()
            questionsListener?.remove()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (partnerName != null) {
                        Text("Chat with $partnerName")
                    } else {
                        Text("Chat")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuestionDialog = true }
            ) {
                Icon(Icons.Default.QuestionAnswer, contentDescription = "Ask a Question")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading chat...")
                }
            } else if (partnerUserId == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Connect with a partner to start chatting",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        MessageItem(
                            message = message,
                            isFromCurrentUser = message.senderId == currentUserId
                        )
                    }
                    
                    // Add couple questions in the chat
                    items(questions) { question ->
                        CoupleQuestionItem(
                            question = question,
                            currentUserId = currentUserId
                        )
                    }
                }
            }
            
            // Message Input
            if (partnerUserId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val message = Message(
                                    id = UUID.randomUUID().toString(),
                                    senderId = currentUserId,
                                    receiverId = partnerUserId!!,
                                    content = messageText,
                                    timestamp = Date(),
                                    read = false
                                )
                                
                                FirebaseFirestore.getInstance().collection("messages")
                                    .document(message.id)
                                    .set(message)
                                
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    
    // Ask Question Dialog
    if (showQuestionDialog) {
        AskQuestionDialog(
            onDismiss = { showQuestionDialog = false },
            onQuestionGenerated = { topic ->
                // Generate a question based on the selected topic
                val question = generateQuestion(topic)
                
                // Add question to Firestore
                val coupleQuestion = CoupleQuestion(
                    id = UUID.randomUUID().toString(),
                    topic = topic,
                    questionText = question,
                    userResponses = mapOf(),
                    timestamp = Date()
                )
                
                FirebaseFirestore.getInstance().collection("coupleQuestions")
                    .document(coupleQuestion.id)
                    .set(coupleQuestion)
                
                showQuestionDialog = false
            }
        )
    }
}

@Composable
fun MessageItem(
    message: Message,
    isFromCurrentUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "P",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromCurrentUser) 16.dp else 0.dp,
                    bottomEnd = if (isFromCurrentUser) 0.dp else 16.dp
                ),
                color = if (isFromCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        if (isFromCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Me",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun CoupleQuestionItem(
    question: CoupleQuestion,
    currentUserId: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (question.topic) {
                QuestionTopic.NAUGHTY -> MaterialTheme.colorScheme.errorContainer
                QuestionTopic.FUNNY -> MaterialTheme.colorScheme.tertiaryContainer
                QuestionTopic.EMOTIONAL -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = question.topic.name.lowercase().replaceFirstChar { it.uppercase() } + " Question",
                style = MaterialTheme.typography.labelLarge,
                color = when (question.topic) {
                    QuestionTopic.NAUGHTY -> MaterialTheme.colorScheme.onErrorContainer
                    QuestionTopic.FUNNY -> MaterialTheme.colorScheme.onTertiaryContainer
                    QuestionTopic.EMOTIONAL -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (question.topic) {
                    QuestionTopic.NAUGHTY -> MaterialTheme.colorScheme.onErrorContainer
                    QuestionTopic.FUNNY -> MaterialTheme.colorScheme.onTertiaryContainer
                    QuestionTopic.EMOTIONAL -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Responses
            question.userResponses.forEach { (userId, response) ->
                val isCurrentUser = userId == currentUserId
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrentUser) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCurrentUser) "Me" else "P",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = response,
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (question.topic) {
                            QuestionTopic.NAUGHTY -> MaterialTheme.colorScheme.onErrorContainer
                            QuestionTopic.FUNNY -> MaterialTheme.colorScheme.onTertiaryContainer
                            QuestionTopic.EMOTIONAL -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Response input if user hasn't answered yet
            if (!question.userResponses.containsKey(currentUserId)) {
                var response by remember { mutableStateOf("") }
                
                OutlinedTextField(
                    value = response,
                    onValueChange = { response = it },
                    placeholder = { Text("Your answer...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        if (response.isNotBlank()) {
                            // Update question with user's response
                            val updatedResponses = question.userResponses.toMutableMap()
                            updatedResponses[currentUserId] = response
                            
                            FirebaseFirestore.getInstance().collection("coupleQuestions")
                                .document(question.id)
                                .update("userResponses", updatedResponses)
                        }
                    },
                    enabled = response.isNotBlank()
                ) {
                    Text("Submit Answer")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskQuestionDialog(
    onDismiss: () -> Unit,
    onQuestionGenerated: (QuestionTopic) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTopic by remember { mutableStateOf(QuestionTopic.FUNNY) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Ask a Fun Question",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Topic Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedTopic.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Topic") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        QuestionTopic.values().forEach { topic ->
                            DropdownMenuItem(
                                text = { Text(topic.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedTopic = topic
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { onQuestionGenerated(selectedTopic) }
                    ) {
                        Text("Generate Question")
                    }
                }
            }
        }
    }
}

private fun generateQuestion(topic: QuestionTopic): String {
    val questions = when (topic) {
        QuestionTopic.NAUGHTY -> listOf(
            "What's your favorite part of my body?",
            "What's a fantasy you've never told me about?",
            "What's something you'd like to try in the bedroom?",
            "Where's the most adventurous place you'd like to be intimate?",
            "What outfit would you love to see me wear?"
        )
        
        QuestionTopic.FUNNY -> listOf(
            "If we were a superhero duo, what would our powers be?",
            "What's the weirdest thing you've caught me doing?",
            "If we switched bodies for a day, what would you do first?",
            "What's your best impression of me?",
            "If we were in a zombie apocalypse, what role would each of us play?"
        )
        
        QuestionTopic.EMOTIONAL -> listOf(
            "When did you first realize you were falling for me?",
            "What's one thing I do that always makes you feel loved?",
            "What's a moment between us that you'll never forget?",
            "How have I helped you grow as a person?",
            "What's one thing you hope we'll accomplish together in the future?"
        )
    }
    
    return questions.random()
}
