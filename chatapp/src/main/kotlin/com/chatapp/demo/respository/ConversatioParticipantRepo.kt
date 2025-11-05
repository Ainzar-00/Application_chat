package com.chatapp.demo.respository

import com.chatapp.demo.model.ConversationParticipant
import com.chatapp.demo.model.ConversationParticipantId
import com.chatapp.demo.model.conversation
import java.util.Optional
import org.springframework.data.domain.Limit
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Interface du dépôt (repository) pour la gestion des entités [ConversationParticipant].
 *
 * Cette interface assure la persistance et la manipulation des participants d’une conversation,
 * en utilisant la clé composite [ConversationParticipantId] comme identifiant principal.
 *
 * Elle hérite de [JpaRepository] pour bénéficier des opérations CRUD standard (création, lecture,
 * mise à jour, suppression) ainsi que de la gestion automatique des transactions.
 */
@Repository
interface ConversationParticipantRepo: JpaRepository<ConversationParticipant,ConversationParticipantId> {

    /**
     * Cette fonction recherche les participants d'une conversation selon leur status(ACTIVE-BLOCKED-MUTED...).
     * @param status le role du participant à rechercher
     * @return la liste des participants correspondant au role
     */
    fun findBystatus(status:String):List<ConversationParticipant>

    /**
     * Cette fonction recherche les participants d'une conversation selon leur role.
     * @param role le role du participant à rechercher
     * @return la liste des participants correspondant au role
     */
    fun findByrole(role:String):List<ConversationParticipant>


    fun findByConversation(conversation: conversation, sort: Sort, limit: Limit): MutableList<ConversationParticipant>

    /**
     *Cette fonction recherche les participants d'une conversation par l'id de conversaion
     * @param conversationId id de conversation
     * @return lise de ConversationParticipant s'il exist et lise vide sinon
     *
     */
    @Query("SELECT p FROM ConversationParticipant p LEFT JOIN FETCH p.user WHERE p.conversation.id = :conversationId")
    fun findByConversationIdWithUser(conversationId: Int): List<ConversationParticipant>

    /**
     ** Recherche un participant specifique dans une conversation donnée.
     *  * @param userId l'identifiant de l'utilisateur
     *  * @param conversationId l'identifiant de la conversation
     *  * @return un Optional contenant le participant si trouvé, ou Optional.empty() sinon
     */
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.id.userId = :userId AND cp.id.conversationId = :conversationId")
    fun findByUserIdAndConversationId(@Param("userId") userId: Int, @Param("conversationId") conversationId: Int): Optional<ConversationParticipant>

    /**
     * Recherche tous les participants correspondant à une conversation ou a un utilisateur donné.
     * @param conversationId l'identifiant de la conversation
     * @param userId l'identifiant de l'utilisateur
     * @return la liste des participants correspondant soit à la conversation, soit à l'utilisateur
     */
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId OR cp.id.userId = :userId")
    fun findByConversationIdOrUserId(@Param("conversationId") conversationId: Int, @Param("userId") userId: Int): List<ConversationParticipant>
}