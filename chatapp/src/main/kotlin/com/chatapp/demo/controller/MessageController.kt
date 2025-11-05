package com.chatapp.demo.controller

import com.chatapp.demo.dto.*
import com.chatapp.demo.service.ConversationService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * Contrôleur REST pour gérer les messages dans une conversation.
 *
 * Les endpoints permettent de :
 * - Envoyer un message
 * - Récupérer les messages d'une conversation
 * - Supprimer un message (utilisateur ou admin)
 *
 * La session HTTP est utilisée pour identifier l'utilisateur courant.
 */
@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
class MessageController(
    private val conversationService: ConversationService
) {

    /**
     * Envoie un message dans une conversation.
     * @param servletReq requête HTTP pour accéder à la session
     * @param conversationId ID de la conversation
     * @param request contenu du message
     * @return ResponseEntity avec ApiResponse contenant les détails du message envoyé
     */
    @PostMapping
    fun sendMessage(
        servletReq: HttpServletRequest,
        @PathVariable conversationId: Int,
        @RequestBody request: CreateMessage
    ): ResponseEntity<out ApiResponse<out Any>?> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val message = conversationService.addTextMessage(
            actorId = actorId,
            conversationId = conversationId,
            content = request.content,
            sentAt = LocalDateTime.now()
        )

        val response = MessageResponse(
            id = message.id!!,
            senderId = message.sender.id,
            content = message.content,
            createdAt = message.created_at
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(true, "Message sent successfully", response))
    }


    /**
     * Récupère tous les messages d'une conversation.
     * @param conversationId ID de la conversation
     * @return ResponseEntity avec ApiResponse contenant la liste des messages
     */
    @GetMapping
    fun getConversationMessages(
        @PathVariable conversationId: Int
    ): ResponseEntity<ApiResponse<List<com.chatapp.demo.dto.MessageResponse>>?> {
        val messages = conversationService.getConversationMessages(conversationId).map { msg ->
            MessageResponse(
                id = msg.id!!,
                senderId = msg.sender?.id,
                content = msg.content,
                createdAt = msg.created_at,
            )
        }

        return ResponseEntity.ok(ApiResponse(true, "Messages retrieved successfully", messages))
    }

    /**
     * Supprime un message par son auteur.
     * @param servletReq requête HTTP pour accéder à la session
     * @param conversationId ID de la conversation
     * @param messageId ID du message à supprimer
     * @return ResponseEntity avec ApiResponse indiquant le succès de l'opération
     */
    @DeleteMapping("/{messageId}")
    fun deleteMessage(
        servletReq: HttpServletRequest,
        @PathVariable conversationId: Int,
        @PathVariable messageId: Int
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.deleteMessage(messageId, actorId)
        return ResponseEntity.ok(ApiResponse(true, "Message deleted successfully", null))
    }

    /**
     * Supprime un message en tant qu'administrateur.
     * @param servletReq requête HTTP pour accéder à la session
     * @param conversationId ID de la conversation
     * @param messageId ID du message à supprimer
     * @return ResponseEntity avec ApiResponse indiquant le succès de l'opération
     */
    @DeleteMapping("/{messageId}/admin")
    fun adminDeleteMessage(
        servletReq: HttpServletRequest,
        @PathVariable conversationId: Int,
        @PathVariable messageId: Int
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.deleteMessageAsAdmin(actorId, messageId)
        return ResponseEntity.ok(ApiResponse(true, "Message deleted by admin successfully", null))
    }


    /**
     * Récupère l'identifiant de l'utilisateur courant depuis la session.
     * @param servletReq requête HTTP pour accéder à la session
     * @return l'ID de l'utilisateur ou null si non authentifié
     */
    private fun getSessionUserId(servletReq: HttpServletRequest): Int? {
        val raw = servletReq.getSession(false)?.getAttribute("userId") ?: return null
        return when (raw) {
            is Int -> raw
            is Long -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        }
    }
}
