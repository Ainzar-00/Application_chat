package com.chatapp.demo.controller

import com.chatapp.demo.dto.*
import com.chatapp.demo.service.ConversationService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Contrôleur REST pour gérer les conversations.
 *
 * Les endpoints permettent de :
 * - Créer des conversations privées ou de groupe
 * - Récupérer des conversations par ID ou par utilisateur
 * - Rechercher des utilisateurs
 * - Gérer les participants (bloquer, débloquer, supprimer)
 * - Mettre à jour le nom d'un groupe
 *
 * La session HTTP est utilisée pour identifier l'utilisateur courant.
 */
@RestController
@RequestMapping("/api/conversations")
class ConversationController(
    private val conversationService: ConversationService
) {

    /**
     * Crée une conversation privée entre l'utilisateur courant et un autre utilisateur.
     * @param servletReq requête HTTP pour accéder à la session
     * @param request contient l'ID de l'autre utilisateur et un nom personnalisé optionnel
     * @return ResponseEntity avec ApiResponse contenant les informations de la conversation créée
     */
    @PostMapping("/private")
    fun createPrivateConversation(
        servletReq: HttpServletRequest,
        @RequestBody request: CreatePrivateConversationRequest
    ): ResponseEntity<ApiResponse<ConversationResponse>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val conversation = conversationService.creatPrivateConversation(
            userId1 = actorId,
            userId2 = request.otherUserId,
            type = "private",
            custom_name1 = request.customName
        )

        val response = ConversationResponse(
            id = conversation.id!!,
            type = conversation.type,
            name = conversation.name,
            createdAt = conversation.created_at,
            participantCount = conversation.participants.size
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(true, "Private conversation created successfully", response))
    }

    /**
     * Crée une conversation de groupe.
     */
    @PostMapping("/group")
    fun createGroupConversation(
        servletReq: HttpServletRequest,
        @RequestBody request: CreateGroupConversationRequest
    ): ResponseEntity<ApiResponse<ConversationResponse>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val conversation = conversationService.creatGroupConversation(
            name = request.name,
            creatorId = actorId,
            type = "group"
        )

        val response = ConversationResponse(
            id = conversation.id!!,
            type = conversation.type,
            name = conversation.name,
            createdAt = conversation.created_at,
            participantCount = conversation.participants.size
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(true, "Group conversation created successfully", response))
    }

    /**
     * Récupère une conversation par son ID.
     */
    @GetMapping("/{id}")
    fun getConversationById(@PathVariable id: Int): ResponseEntity<ApiResponse<ConversationResponse>> {
        val conversation = conversationService.getConversationById(id)

        val response = ConversationResponse(
            id = conversation.id!!,
            type = conversation.type,
            name = conversation.name,
            createdAt = conversation.created_at,
            participantCount = conversation.participants.size
        )

        return ResponseEntity.ok(ApiResponse(true, "Conversation retrieved successfully", response))
    }

    /**
     * Recherche des utilisateurs par nom, email ou téléphone.
     */
    @GetMapping("/search/users")
    fun searchUsers(@RequestParam query: String): ResponseEntity<ApiResponse<List<UserResponse>>> {
        val users = conversationService.searchUsers(query)
        val responses = users.map { UserResponse(it.id!!, it.username, it.email, it.phone) }
        return ResponseEntity.ok(ApiResponse(true, "Users found", responses))
    }

    /**
     * Récupère toutes les conversations de l'utilisateur courant.
     */
    @GetMapping("/user")
    fun getUserConversations(
        servletReq: HttpServletRequest
    ): ResponseEntity<ApiResponse<List<ConversationResponse>>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val conversations = conversationService.getUserConversations(actorId).map { conv ->
            ConversationResponse(
                id = conv.id!!,
                type = conv.type,
                name = conv.name,
                createdAt = conv.created_at,
                participantCount = conv.participants.size
            )
        }

        return ResponseEntity.ok(ApiResponse(true, "Conversations retrieved successfully", conversations))
    }

    /**
     * Récupère les conversations avec messages (chats) de l'utilisateur courant.
     */
    @GetMapping("/chats")
    fun getUserChats(
        servletReq: HttpServletRequest
    ): ResponseEntity<ApiResponse<List<ConversationResponse>>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val conversations = conversationService.getUserChats(actorId).map { conv ->
            ConversationResponse(
                id = conv.id!!,
                type = conv.type,
                name = conv.name,
                createdAt = conv.created_at,
                participantCount = conv.participants.size
            )
        }
        return ResponseEntity.ok(ApiResponse(true, "Chats retrieved successfully", conversations))
    }

    /**
     * Récupère les contacts (conversations sans messages) de l'utilisateur courant.
     */
    @GetMapping("/contacts")
    fun getUserContacts(
        servletReq: HttpServletRequest
    ): ResponseEntity<ApiResponse<List<ConversationResponse>>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val conversations = conversationService.getUserContacts(actorId).map { conv ->
            ConversationResponse(
                id = conv.id!!,
                type = conv.type,
                name = conv.name,
                createdAt = conv.created_at,
                participantCount = conv.participants.size
            )
        }

        return ResponseEntity.ok(ApiResponse(true, "Contacts retrieved successfully", conversations))
    }

    /**
     * Bloque un participant dans une conversation.
     */
    @PatchMapping("/participants/block")
    fun blockParticipant(
        servletReq: HttpServletRequest,
        @RequestBody request: BlockingOptions
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.blockUser(actorId=actorId, conversationId= request.conversationId, userId=request.targetUserId)
        return ResponseEntity.ok(ApiResponse(true, "User blocked successfully", null))
    }

    /**
     * Débloque un participant dans une conversation.
     */
    @PatchMapping("/participants/deblock")
    fun deblockParticipant(
        servletReq: HttpServletRequest,
        @RequestBody request: BlockingOptions
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.deblockUser(actorId, request.conversationId, request.targetUserId)
        return ResponseEntity.ok(ApiResponse(true, "User deblocked successfully", null))
    }

    /**
     * Met à jour le nom d'un groupe.
     */
    @PutMapping("/{id}/name")
    fun updateGroupName(
        servletReq: HttpServletRequest,
        @RequestBody request: UpdateGroupNameRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.updateGroupConversationName(actorId, request.conversationId, request.name)
        return ResponseEntity.ok(ApiResponse(true, "Group name updated successfully", null))
    }

    /**
     * Supprime une conversation.
     */

    @DeleteMapping("/participant")
    fun deleteConversation(
        servletReq: HttpServletRequest,
        @RequestBody request: DeleteParticipantConversation
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.deleteUserConversation(actorId, request.conversationId,request.targetUserId)
        return ResponseEntity.ok(ApiResponse(true, "Conversation participant removed successfully", null))
    }
    /**
     * Récupère l'identifiant de l'utilisateur courant depuis la session.
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
