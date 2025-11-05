package com.chatapp.demo.respository

import com.chatapp.demo.model.conversation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Interface du dépôt (repository) pour la gestion des entités [conversation].
 *
 * Cette interface permet la persistance, la récupération et la gestion
 * des conversations entre utilisateurs.
 *
 * Elle hérite de [JpaRepository] afin de bénéficier des opérations CRUD standard
 * (création, lecture, mise à jour, suppression) ainsi que du support des requêtes dérivées.
 */
@Repository
interface ConversationRepo: JpaRepository<conversation,Int>{

    /**
     *Cette fonction recherche une conversation par son nom
     * @param name nom de conversation
     *@return list des conversations ou une liste vide
     */
    fun findByName(name:String):List<conversation>

    /**
     *Cette fonction vérifie s’il existe une conversation privee entre deux utilisateurs
     * @param userId1
     * @param userId2
     * @return true s'il existe ou false sinon
     */
    @Query(
        """
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END 
        FROM conversation c 
        JOIN c.participants cp1 
        JOIN c.participants cp2 
        WHERE c.type = 'private' 
        AND cp1.user.id = :userId1 
        AND cp2.user.id = :userId2 
        AND cp1.user.id != cp2.user.id
        """
    )
    fun existsPrivateConversationBetweenUsers(
        @Param("userId1") userId1: Int?,
        @Param("userId2") userId2: Int?
    ): Boolean

    /**
     *Cette fonction recherches des conversations par un participant id
     * @param userId l'indentifiant de l'utilisateur
     * @return list des conversations ou une list vide
     */
    @Query("SELECT c FROM conversation c JOIN c.participants p WHERE p.user.id = :userId")
    fun findByParticipantsUserId(@Param("userId") userId: Int): List<conversation>

    /**
     *Cette fonction recherche les conversations qui  contiennent au moins un message
     * @param userId l'indentifiant de l'utilisateur
     * @return list des conversations ou une list vide
     */
    @Query("""
    SELECT c FROM conversation c 
    WHERE EXISTS (
        SELECT m FROM message m 
        WHERE m.conversation.id = c.id 
        AND EXISTS (
            SELECT cp FROM ConversationParticipant cp 
            WHERE cp.conversation.id = c.id 
            AND cp.id.userId = :userId
        )
    )
""")
    fun findConversationsWithMessages(@Param("userId") userId: Int): List<conversation>

    /**
     *Cette fonction recherche les conversations qui ne contiennent aucun message
     * @param userId l'indentifiant de l'utilisateur
     * @return list des conversations ou une list vide
     */
    @Query("""
        SELECT c FROM conversation c 
        WHERE NOT EXISTS (
            SELECT 1 FROM message m 
            WHERE m.conversation.id = c.id
        )
        AND EXISTS (
            SELECT 1 FROM ConversationParticipant cp 
            WHERE cp.conversation.id = c.id 
            AND cp.id.userId = :userId
        )
        ORDER BY c.created_at DESC
    """)
    fun findConversationsWithoutMessages(@Param("userId") userId: Int): List<conversation>

    /**
     * Recherche des conversations par type et par nom.
     * @param type le type de conversation (par exemple "private" ou "group")
     * @param query la chaîne de recherche à comparer au nom de la conversation
     * @param pageable les paramètres de pagination (page, taille, tri, etc.)
     * @return la liste des conversations correspondant au type et dont le nom contient la valeur
     */
    @Query("SELECT c FROM conversation c WHERE c.type = :type AND LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun searchByTypeAndName(@Param("type") type: String, @Param("query") query: String,  pageable: Pageable): List<conversation>
}

