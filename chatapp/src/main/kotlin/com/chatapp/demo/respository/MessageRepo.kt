package com.chatapp.demo.respository

import com.chatapp.demo.model.message
import com.chatapp.demo.model.message_text
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Interface du dépôt (repository) pour la gestion des entités [message].
 *
 * Cette interface fournit les opérations CRUD ainsi que plusieurs méthodes de requêtes
 * personnalisées pour récupérer et gérer les messages selon les conversations
 * ou les expéditeurs.
 *
 * Elle étend [JpaRepository] afin de bénéficier des fonctionnalités de persistance JPA standard.
 */
@Repository
interface MessageRepo: JpaRepository<message,Int> {

    /**
     * Récupère tous les messages appartenant à une conversation spécifique.
     * @param conversationId l'identifiant de la conversation.
     * @return une liste d'entités [message] associées à la conversation donnée.
     */
    fun findByConversationId(conversationId:Int):List<message>

    /**
     * Récupère tous les messages envoyés par un utilisateur spécifique.
     * @param senderId l'identifiant de l’expéditeur du message.
     * @return une liste d'entités [message] envoyées par l’utilisateur spécifié.
     */
    fun findBySenderId(senderId:Int):List<message>

    /**
     * Récupère tous les messages appartenant à une conversation identifiée par son nom.
     * @param conversationName le nom de la conversation.
     * @return une liste d'entités [message] associées au nom de conversation indiqué.
     */
    fun findByConversationName(conversationName:String):List<message>

    /**
     * Récupère tous les messages envoyés par un utilisateur identifié par son nom d’utilisateur.
     * @param senderName le nom d’utilisateur de l’expéditeur.
     * @return une liste d'entités [message] envoyées par le nom d’utilisateur spécifié.
     */
    fun findBySenderUsername(senderName:String):List<message>

    /**
     * Supprime tous les messages appartenant à une conversation donnée.
     * @param id l’identifiant de la conversation dont les messages doivent être supprimés.
     */
    fun deleteByConversationId(id:Int)

    /**
     * Récupère tous les messages provenant des conversations auxquelles un utilisateur participe.
     * Cette requête sélectionne tous les identifiants de conversation contenant l’utilisateur indiqué,
     * puis renvoie tous les messages associés à ces conversations.
     * @param userId l’identifiant de l’utilisateur.
     * @return une liste de [message] appartenant aux conversations du participant.
     */
    @Query("""
    SELECT m FROM message m 
    WHERE m.conversation.id IN (
        SELECT p.conversation.id FROM ConversationParticipant p WHERE p.user.id = :userId
    )
""")
    fun findAllUserMessages(@Param("userId") userId: Int): List<message>

    /**
     * Récupère tous les messages textuels d’une conversation spécifique,
     * avec les informations de l’expéditeur chargées en même temps (fetch join).
     * Les messages sont triés par date de création.
     * @param conversationId l’identifiant de la conversation.
     * @return une liste de [message_text] contenant les expéditeurs associés.
     */
    @Query("SELECT m FROM message_text m LEFT JOIN FETCH m.sender WHERE m.conversation.id = :conversationId ORDER BY m.created_at")
    fun findByConversationIdWithSender(conversationId: Int): List<message_text>
}

