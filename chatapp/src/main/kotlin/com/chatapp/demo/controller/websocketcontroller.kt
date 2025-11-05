package com.chatapp.demo.controller

import com.chatapp.demo.service.ConversationService
import com.chatapp.demo.service.UserService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.time.LocalDateTime


/**
 * Représente un message envoyé par un client via WebSocket.
 * @property conversationId identifiant de la conversation ciblée.
 * @property senderId identifiant de l'utilisateur émetteur (doit correspondre à l'utilisateur authentifié).
 * @property content contenu textuel du message.
 * @property timestamp horodatage en millisecondes côté client (valeur par défaut : maintenant).
 */
data class ChatMessage(
    val conversationId: Int,
    val senderId: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Représente la réponse diffusée aux abonnés d'une conversation après sauvegarde du message.
 * Cette structure contient les informations utile côté client pour afficher le message.
 * @property id identifiant du message persisté.
 * @property conversationId identifiant de la conversation.
 * @property senderId identifiant de l'expéditeur.
 * @property senderName nom d'affichage de l'expéditeur.
 * @property content contenu textuel du message.
 * @property timestamp date et heure de création côté serveur.
 */
data class MessageResponse(
    val id: Int,
    val conversationId: Int,
    val senderId: Int,
    val senderName: String,
    val content: String,
    val timestamp: LocalDateTime
)

/**
 * Contrôleur WebSocket responsable de la réception des messages entrants
 * et de la diffusion vers les topics appropriés.
 *
 * Le contrôleur :
 * 1. Vérifie que l'utilisateur est authentifié et que l'ID d'expéditeur transmis
 *    correspond à l'utilisateur connecté.
 * 2. Persiste le message via [ConversationService].
 * 3. Récupère les informations de l'utilisateur via [UserService].
 * 4. Diffuse le message persisté sur le topic `/topic/conversation/{conversationId}`.
 *
 * @property messagingTemplate composant Spring pour envoyer des messages STOMP/WebSocket.
 * @property messageService service métier pour gérer les messages/conversations.
 * @property userService service métier pour récupérer les informations utilisateur.
 */
@Controller
class WebSocketController(
    private val messagingTemplate: SimpMessagingTemplate,
    private val messageService: ConversationService,  // Inject MessageService
    private val userService: UserService  // Inject UserService to get username
) {

    /**
     * Point d'entrée pour les messages envoyés par les clients.
     * Les clients publient sur l'endpoint `/app/chat.send` (routing côté STOMP).
     * Cette méthode :
     * - récupère l'utilisateur authentifié depuis [SimpMessageHeaderAccessor],
     * - valide la correspondance entre l'utilisateur authentifié et le champ `senderId` du message,
     * - enregistre le message en base,
     * - construit une [MessageResponse] et la diffuse sur le topic `/topic/conversation/{conversationId}`.
     *
     * @param message objet reçu depuis le client contenant le contenu et les métadonnées.
     * @param headerAccessor accesseur aux en-têtes / principal de la connexion WebSocket.
     * @throws IllegalStateException si l'utilisateur n'est pas authentifié ou si la sauvegarde échoue.
     * @throws IllegalArgumentException si l'utilisateur tente d'envoyer un message au nom d'un autre utilisateur.
     */

    @MessageMapping("/chat.send")
    fun sendMessage(
        @Payload message: ChatMessage,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        println("=== Received message ===")
        println("Message: $message")
        println("User principal: ${headerAccessor.user}")
        println("User principal name: ${headerAccessor.user?.name}")

        // Get authenticated user ID from principal (stored as String)
        val userIdString = headerAccessor.user?.name

        if (userIdString == null) {
            println("❌ ERROR: User not authenticated")
            throw IllegalStateException("User not authenticated")
        }

        val authenticatedUserId = try {
            userIdString.toInt()
        } catch (e: NumberFormatException) {
            println("❌ ERROR: Invalid user ID format: $userIdString")
            throw IllegalStateException("Invalid user ID")
        }

        println("Authenticated user ID: $authenticatedUserId")
        println("Message senderId: ${message.senderId}")

        // Verify the sender ID matches authenticated user
        if (message.senderId != authenticatedUserId) {
            println("❌ ERROR: Sender mismatch! Authenticated: $authenticatedUserId, Claimed: ${message.senderId}")
            throw IllegalArgumentException(
                "Cannot send message as different user. You are user $authenticatedUserId but tried to send as ${message.senderId}"
            )
        }

        println("✓ Sender verified: $authenticatedUserId")

        try {
            // 1. Save the message to database
            val savedMessage = messageService.addTextMessage(
                actorId = authenticatedUserId,
                content = message.content,
                conversationId = message.conversationId,
                sentAt = LocalDateTime.now()
            )

            println("✓ Message saved to database with ID: ${savedMessage.id}")

            // 2. Get the sender's username
            val sender = userService.finduserById(authenticatedUserId)
            val senderName = sender.username

            // 3. Create response with actual saved message data
            val response = MessageResponse(
                id = savedMessage.id!!,
                conversationId = savedMessage.conversation.id!!,
                senderId = authenticatedUserId,
                senderName = senderName,
                content = savedMessage.content!!,
                timestamp = savedMessage.created_at
            )

            println("✓ Broadcasting message to /topic/conversation/${message.conversationId}")

            // 4. Send to specific conversation topic
            messagingTemplate.convertAndSend(
                "/topic/conversation/${message.conversationId}",
                response
            )

            println("=== Message sent successfully ===")

        } catch (e: Exception) {
            println("❌ ERROR saving or broadcasting message: ${e.message}")
            e.printStackTrace()
            throw IllegalStateException("Failed to save message: ${e.message}")
        }
    }

    /**
     * Envoie un message privé à un utilisateur spécifique.
     * Cette méthode utilise la destination `/queue/user/{userId}` ; l'utilisateur
     * doit être abonné à cette queue pour recevoir le message privé.
     * @param userId identifiant de l'utilisateur destinataire.
     * @param message payload à envoyer.
     */
    fun sendPrivateMessage(userId: Int, message: Any) {
        messagingTemplate.convertAndSend("/queue/user/$userId", message)
    }
}