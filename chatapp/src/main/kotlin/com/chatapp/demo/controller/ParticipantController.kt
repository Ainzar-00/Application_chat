package com.chatapp.demo.controller

import com.chatapp.demo.dto.*
import com.chatapp.demo.service.ConversationService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Contrôleur REST pour gérer les participants des conversations.
 * Les endpoints permettent de :
 * - Ajouter un participant à une conversation
 * - Récupérer les participants d'une conversation
 * - Supprimer un participant d'une conversation
 * La session HTTP est utilisée pour identifier l'utilisateur courant.
 */
@RestController
@RequestMapping("/api/conversations/{conversationId}/participants")
class ParticipantController(
    private val conversationService: ConversationService
) {

    /**
     * Ajoute un participant à une conversation.
     * @param servletReq requête HTTP pour accéder à la session
     * @param conversationId ID de la conversation
     * @param request contient l'ID de l'utilisateur à ajouter et son rôle
     * @return ResponseEntity avec ApiResponse indiquant le succès de l'opération
     */
    @PostMapping
    fun addParticipant(
        servletReq: HttpServletRequest,
        @PathVariable conversationId: Int,
        @RequestBody request: AddParticipantRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.addParticipant(actorId, conversationId, request.userId, request.role)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(true, "Participant added successfully", null))
    }

    /**
     * Récupère tous les participants d'une conversation.
     * @param conversationId ID de la conversation
     * @return ResponseEntity avec ApiResponse contenant la liste des participants
     */
    @GetMapping
    fun getConversationParticipants(
        @PathVariable conversationId: Int
    ): ResponseEntity<ApiResponse<List<ParticipantResponse>>> {
        val participants = conversationService.getConversationParticipants(conversationId).map { p ->
            ParticipantResponse(
                id = p.id!!,
                userId = p.user?.id,
                username = p.user?.username,
                role = p.role,
                status = p.status,
                joinedAt = p.joinat
            )
        }

        return ResponseEntity.ok(ApiResponse(true, "Participants retrieved successfully", participants))
    }

    /**
     * Supprime un participant d'une conversation.
     * @param servletReq requête HTTP pour accéder à la session
     * @param conversationId ID de la conversation
     * @param targetUserId ID de l'utilisateur à supprimer
     * @return ResponseEntity avec ApiResponse indiquant le succès de l'opération
     */
    @DeleteMapping("/{targetUserId}")
    fun removeParticipant(
        servletReq: HttpServletRequest,
        @PathVariable conversationId: Int,
        @PathVariable targetUserId: Int
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.deleteParticipant(actorId, conversationId, targetUserId)
        return ResponseEntity.ok(ApiResponse(true, "Participant removed successfully", null))
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