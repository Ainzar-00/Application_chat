package com.chatapp.demo.service

import com.chatapp.demo.model.ConversationParticipant
import com.chatapp.demo.model.User
import com.chatapp.demo.respository.MessageRepo
import com.chatapp.demo.model.message_text
import com.chatapp.demo.model.conversation
import com.chatapp.demo.respository.ConversationRepo
import com.chatapp.demo.respository.ConversationParticipantRepo
import com.chatapp.demo.respository.UserRepo
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.ResponseStatus
import java.time.LocalDateTime

/**
 * Exception levée lorsqu'une conversation privée existe déjà entre deux utilisateurs.
 *
 * Cette exception renvoie le statut HTTP 409 (CONFLICT) grâce à l'annotation [ResponseStatus].
 *
 * @param message message décrivant l'erreur.
 */
@ResponseStatus(HttpStatus.CONFLICT)
class ConversationAlreadyExistsException(message: String) : RuntimeException(message)

/**
 * Service de gestion des conversations.
 *
 * Ce service encapsule la logique métier relative à la création, la recherche,
 * la modification et la suppression des conversations, des participants et des messages.
 *
 * Les transactions sont gérées au niveau de la classe (annotation [Transactional]).
 *
 * @property conversationRepo dépôt des conversations.
 * @property messageRepo dépôt des messages.
 * @property conversationParticipantRepo dépôt des participants de conversation.
 * @property userRepo dépôt des utilisateurs.
 */
@Service
@Transactional
class ConversationService(
    private val conversationRepo: ConversationRepo,
    private val messageRepo: MessageRepo,
    private val conversationParticipantRepo: ConversationParticipantRepo,
    private val userRepo: UserRepo
) {

    /**
     * Vérifie si une conversation privée existe déjà entre deux utilisateurs.
     * @param userId1 identifiant du premier utilisateur (nullable).
     * @param userId2 identifiant du second utilisateur (nullable).
     * @return `true` si une conversation privée existe déjà, sinon `false`.
     */
    fun hasPrivateConversation(userId1: Int?, userId2: Int?): Boolean {
        return conversationRepo.existsPrivateConversationBetweenUsers(userId1, userId2)
    }


    /**
     * Crée une conversation privée entre deux utilisateurs.
     * Validations effectuées :
     * - les identifiants ne peuvent pas être nuls,
     * - un utilisateur ne peut pas s'envoyer une conversation à lui-même,
     * - si une conversation privée existe déjà, une exception [ConversationAlreadyExistsException] est levée.
     * Le créateur (user1) et l'autre participant (user2) sont ajoutés comme participants.
     * @param userId1 identifiant du premier utilisateur (créateur).
     * @param userId2 identifiant du second utilisateur.
     * @param type type de conversation (ex. "private").
     * @param custom_name1 nom personnalisé affiché pour user1 (optionnel).
     * @return la [conversation] nouvellement créée et persistée.
     * @throws IllegalArgumentException si les identifiants sont invalides ou si un utilisateur est introuvable.
     * @throws ConversationAlreadyExistsException si une conversation privée existe déjà entre ces deux utilisateurs.
     */
    fun creatPrivateConversation(userId1: Int?, userId2: Int?, type: String, custom_name1: String?): conversation {
        if (userId1 == null || userId2 == null) {
            throw IllegalArgumentException("User IDs cannot be null")
        }

        if (userId1 == userId2) {
            throw IllegalArgumentException("Cannot create conversation with yourself")
        }

        val possibleconversation = hasPrivateConversation(userId1, userId2)
        if (possibleconversation) {
            throw ConversationAlreadyExistsException("Private conversation already exists")
        }

        val user1 = userRepo.findById(userId1).orElseThrow { IllegalArgumentException("User1 not found") }
        val user2 = userRepo.findById(userId2).orElseThrow { IllegalArgumentException("User2 not found") }

        val conversation = conversation(type = type, created_by = user1, name = null)

        val participant1 = if (custom_name1 == null) {
            ConversationParticipant(conversation = conversation, user = user1, role = null, status = "ACTIVE", customName = user2.phone)
        } else {
            ConversationParticipant(conversation = conversation, user = user1, role = null, status = "ACTIVE", customName = custom_name1)
        }

        val participant2 = ConversationParticipant(conversation = conversation, user = user2, role = null, status = "ACTIVE", customName = user1.phone)

        conversation.participants.add(participant1)
        conversation.participants.add(participant2)

        return conversationRepo.save(conversation)
    }

    /**
     * Crée une conversation de groupe.
     * Le nom est validé via `ValidationHelpers.generalHelpers.validatename`.
     * Le créateur devient automatiquement administrateur (role = "Admin").
     * @param name nom du groupe.
     * @param creatorId identifiant du créateur (utilisateur).
     * @param type type de conversation (ex. "group").
     * @return la [conversation] de groupe persistée.
     * @throws IllegalArgumentException si le créateur est introuvable.
     */
    fun creatGroupConversation(name: String, creatorId: Int, type: String): conversation {
        val validatedname = ValidationHelpers.generalHelpers.validatename(name)
        val creator = userRepo.findById(creatorId).orElseThrow { IllegalArgumentException("User not found") }

        val conversation = conversation(type = type, name = validatedname, created_by = creator)
        val admin = ConversationParticipant(conversation = conversation, user = creator, role = "Admin", status = "ACTIVE")

        conversation.participants.add(admin)
        return conversationRepo.save(conversation)
    }

    /**
     * Récupère une conversation par son identifiant.
     * @param conversationId identifiant de la conversation.
     * @return la [conversation] trouvée.
     * @throws IllegalArgumentException si la conversation n'existe pas.
     */
    fun getConversationById(conversationId: Int): conversation {
        return conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found") }
    }


    /**
     * Récupère toutes les conversations d'un utilisateur (où il est participant).
     * @param userId identifiant de l'utilisateur.
     * @return liste de [conversation].
     */
    fun getUserConversations(userId: Int): List<conversation> {
        return conversationRepo.findByParticipantsUserId(userId)
    }

    /**
     * Recherche des utilisateurs par requête sur nom d'utilisateur, e-mail ou téléphone.
     *
     * @param query chaîne de recherche.
     * @return jusqu'à 10 utilisateurs correspondant à la requête (liste vide si la requête est vide).
     */
    fun searchUsers(query: String): List<User> {
        if (query.isBlank()) return emptyList()
        val pageable = PageRequest.of(0, 10)
        return userRepo.searchByUsernameEmailOrPhone(query, pageable)
    }


    /**
     * Récupère les conversations correspondant exactement au nom fourni.
     * @param name nom de la conversation.
     * @return liste de [conversation] portant ce nom.
     */
    fun getConversationsByname(name: String): List<conversation> {
        return conversationRepo.findByName(name)
    }

    /**
     * Récupère les conversations d'un utilisateur qui contiennent des messages.
     * @param userId identifiant de l'utilisateur.
     * @return liste de [conversation] avec messages.
     */
    fun getUserChats(userId: Int): List<conversation> {
        return conversationRepo.findConversationsWithMessages(userId)
    }

    /**
     * Récupère les conversations d'un utilisateur qui n'ont pas (encore) de messages.
     * @param userId identifiant de l'utilisateur.
     * @return liste de [conversation] sans messages.
     */
    fun getUserContacts(userId: Int): List<conversation> {
        return conversationRepo.findConversationsWithoutMessages(userId)
    }

    /**
     * Récupère la liste des participants d'une conversation en chargeant les utilisateurs associés.
     * Méthode en lecture seule (annotée [Transactional(readOnly = true)]).
     * @param conversationId identifiant de la conversation.
     * @return liste de [ConversationParticipant] avec information utilisateur.
     */
    @Transactional(readOnly = true)
    fun getConversationParticipants(conversationId: Int): List<ConversationParticipant> {
        return conversationParticipantRepo.findByConversationIdWithUser(conversationId)
    }

    /**
     * Récupère les messages textuels d'une conversation en incluant l'expéditeur (fetch).
     * Méthode en lecture seule.
     * @param conversationId identifiant de la conversation.
     * @return liste de [message_text] triés par date de création.
     */
    @Transactional(readOnly = true)
    fun getConversationMessages(conversationId: Int): List<message_text> {
        return messageRepo.findByConversationIdWithSender(conversationId)
    }

    /**
     * Ajoute un message textuel à une conversation existante.
     * Vérifications :
     * - la conversation et l'utilisateur doivent exister,
     * - l'utilisateur doit être participant de la conversation.
     * Met à jour la date du dernier message via [updateLastMessage].
     * @param actorId identifiant de l'expéditeur.
     * @param conversationId identifiant de la conversation.
     * @param content contenu textuel du message.
     * @param sentAt date d'envoi (utilisée pour la mise à jour du dernier message).
     * @return le [message_text] persistant créé.
     * @throws IllegalArgumentException si la conversation, l'utilisateur n'existent pas ou si l'expéditeur n'est pas participant.
     */
    fun addTextMessage(actorId: Int, conversationId: Int, content: String, sentAt: LocalDateTime): message_text {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found") }

        val sender = userRepo.findById(actorId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val isParticipant = conversation.participants.any { it.user?.id == actorId }
        if (!isParticipant) {
            throw IllegalArgumentException("Sender is not a participant in this conversation")
        }

        val message = message_text(content = content, conversation = conversation, sender = sender)
        updateLastMessage(conversationId, sentAt)
        return messageRepo.save(message)
    }


    /**
     * Ajoute un participant à une conversation.
     * Conditions :
     * - l'acteur doit être participant et avoir le rôle "Admin".
     * - l'utilisateur à ajouter ne doit pas déjà être participant.
     * @param actorId identifiant de l'acteur effectuant l'ajout.
     * @param conversationId identifiant de la conversation.
     * @param userId identifiant de l'utilisateur à ajouter.
     * @param role rôle attribué au nouvel utilisateur.
     * @throws IllegalArgumentException si la conversation ou l'utilisateur n'existent pas, ou si l'acteur n'est pas participant.
     * @throws IllegalStateException si l'acteur n'est pas admin ou si l'utilisateur est déjà participant.
     */
    @Transactional
    fun addParticipant(actorId: Int, conversationId: Int, userId: Int, role: String) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found") }

        val actorParticipant = conversation.participants.find { it.user?.id == actorId }
            ?: throw IllegalArgumentException("Actor is not a participant in conversation $conversationId")

        if (actorParticipant.role != "Admin") {
            throw IllegalStateException("Only admins can add participants")
        }

        val user = userRepo.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        if (conversation.participants.any { it.user?.id == userId }) {
            throw IllegalStateException("User $userId is already a participant")
        }

        val participant = ConversationParticipant(conversation = conversation, user = user, status = "ACTIVE", role = role)
        conversation.participants.add(participant)
        conversationRepo.save(conversation)
    }

    /**
     * Met à jour la date du dernier message d'une conversation.
     * @param conversationId identifiant de la conversation.
     * @param sentAt date et heure du dernier message.
     * @throws IllegalArgumentException si la conversation n'existe pas.
     */
    @Transactional
    fun updateLastMessage(conversationId: Int, sentAt: LocalDateTime) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found") }

        conversation.lastMessageAt = sentAt
        conversationRepo.save(conversation)
    }

    /**
     * Bloque un utilisateur dans une conversation (statut = "BLOCKED").
     * @param actorId identifiant de l'acteur (non utilisé dans la version actuelle mais conservé pour contrôle futur).
     * @param conversationId identifiant de la conversation.
     * @param userId identifiant de l'utilisateur à bloquer.
     * @return `true` si l'opération a réussi.
     * @throws IllegalArgumentException si le participant n'existe pas.
     * @throws IllegalStateException si l'utilisateur est déjà bloqué.
     */
    @Transactional
    fun blockUser(actorId: Int, conversationId: Int, userId: Int): Boolean {

        val participantToBlock = conversationParticipantRepo.findByUserIdAndConversationId(userId, conversationId)
            .orElseThrow { IllegalArgumentException("Participant not found") }

        if (participantToBlock.status == "BLOCKED") {
            throw IllegalStateException("User $userId is already blocked in conversation $conversationId")
        }

        participantToBlock.status = "BLOCKED"
        conversationParticipantRepo.save(participantToBlock)
        return true
    }

    /**
     * Débloque un utilisateur dans une conversation (statut = "ACTIVE") si nécessaire.
     * @param actorId identifiant de l'acteur (conservé pour cohérence).
     * @param conversationId identifiant de la conversation.
     * @param userId identifiant de l'utilisateur à débloquer.
     * @throws IllegalArgumentException si le participant n'existe pas.
     */
    @Transactional
    fun deblockUser(actorId: Int, conversationId: Int, userId: Int) {

        val participant = conversationParticipantRepo.findByUserIdAndConversationId(userId, conversationId)
            .orElseThrow { IllegalArgumentException("Participant not found") }

        if (participant.status == "BLOCKED") {
            participant.status = "ACTIVE"
            conversationParticipantRepo.save(participant)
        }
    }

    /**
     * Met à jour le nom d'une conversation de groupe.
     * L'acteur doit être participant de la conversation.
     * @param actorId identifiant de l'acteur.
     * @param conversationId identifiant de la conversation.
     * @param newName nouveau nom (nullable) : si non null, remplace le nom existant.
     * @throws IllegalArgumentException si la conversation n'existe pas ou si l'acteur n'est pas participant.
     */
    fun updateGroupConversationName(actorId: Int, conversationId: Int, newName: String?) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found") }

        val actorParticipant = conversation.participants.find { it.user?.id == actorId }
            ?: throw IllegalArgumentException("Actor is not a participant in this conversation")


        if (newName != null) conversation.name = newName
        conversationRepo.save(conversation)
    }

    /**
     * Supprime la conversation d'un utilisateur (retire l'utilisateur de la liste des participants).
     * Si l'acteur tente de supprimer la conversation d'un autre utilisateur, il doit être Admin.
     * @param actorId identifiant de l'acteur.
     * @param conversationId identifiant de la conversation.
     * @param targetUserId identifiant de l'utilisateur cible à supprimer de la conversation.
     * @throws IllegalArgumentException si la conversation n'existe pas, si l'acteur n'est pas participant ou si la cible n'est pas participante.
     * @throws IllegalStateException si l'acteur n'est pas admin et tente de supprimer un autre participant.
     */
    @Transactional
    fun deleteUserConversation(actorId: Int, conversationId: Int, targetUserId: Int) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found: $conversationId") }

        val actorIsParticipant = conversation.participants.any { it.user?.id == actorId }
        if (!actorIsParticipant) throw IllegalArgumentException("Actor is not a participant in conversation $conversationId")

        val participantToRemove = conversation.participants.find { it.user?.id == targetUserId }
            ?: throw IllegalArgumentException("User $targetUserId is not a participant in conversation $conversationId")

        if (actorId != targetUserId) {
            val actorParticipant = conversation.participants.find { it.user?.id == actorId }
                ?: throw IllegalArgumentException("Actor participant record not found")
            if (actorParticipant.role != "Admin") {
                throw IllegalStateException("Only admins can remove other participants")
            }
        }

        conversation.participants.remove(participantToRemove)
        conversationRepo.save(conversation)
    }

    /**
     * Supprime un participant d'une conversation.
     * Cas pris en charge :
     * - suppression de soi-même (tout participant peut se retirer),
     * - suppression d'un autre participant (nécessite d'être Admin et la cible ne doit pas être Admin).
     * @param actorId identifiant de l'acteur.
     * @param conversationId identifiant de la conversation.
     * @param targetUserId identifiant du participant à supprimer.
     * @throws IllegalArgumentException si la conversation, l'acteur ou la cible n'existent pas.
     * @throws IllegalStateException si les règles de privilèges (Admin) ne sont pas respectées.
     */
    @Transactional
    fun deleteParticipant(actorId: Int, conversationId: Int, targetUserId: Int) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found: $conversationId") }

        val actorParticipant = conversation.participants.find { it.user?.id == actorId }
            ?: throw IllegalArgumentException("Actor is not a participant in conversation $conversationId")

        val targetParticipant = conversation.participants.find { it.user?.id == targetUserId }
            ?: throw IllegalArgumentException("User $targetUserId is not a participant in conversation $conversationId")

        if (actorId == targetUserId) {
            conversation.participants.remove(targetParticipant)
            conversationRepo.save(conversation)
            return
        }

        if (actorParticipant.role != "Admin") {
            throw IllegalStateException("Only admins can remove other participants")
        }

        if (targetParticipant.role == "Admin") {
            throw IllegalStateException("Cannot remove another admin")
        }

        conversation.participants.remove(targetParticipant)
        conversationRepo.save(conversation)
    }

    /**
     * Promote un participant au rôle "Admin".
     * Seuls les Admins peuvent promouvoir d'autres participants.
     * @param actorId identifiant de l'acteur réalisant la promotion.
     * @param conversationId identifiant de la conversation.
     * @param targetUserId identifiant du participant à promouvoir.
     * @throws IllegalArgumentException si la conversation, l'acteur ou la cible n'existent pas.
     * @throws IllegalStateException si l'acteur n'est pas Admin.
     */
    @Transactional
    fun promoteToAdmin(actorId: Int, conversationId: Int, targetUserId: Int) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found: $conversationId") }

        val actorParticipant = conversation.participants.find { it.user?.id == actorId }
            ?: throw IllegalArgumentException("Actor is not a participant in conversation $conversationId")

        if (actorParticipant.role != "Admin") {
            throw IllegalStateException("Only admins can promote participants")
        }

        val targetParticipant = conversation.participants.find { it.user?.id == targetUserId }
            ?: throw IllegalArgumentException("Target user is not a participant in conversation $conversationId")

        targetParticipant.role = "Admin"
        conversationParticipantRepo.save(targetParticipant)
    }

    /**
     * Supprime un message en tant qu'Admin.
     * L'acteur doit être Admin dans la conversation du message ciblé.
     * @param actorId identifiant de l'acteur demandant la suppression.
     * @param messageId identifiant du message à supprimer.
     * @throws IllegalArgumentException si le message ou la conversation ou le participant n'existent pas.
     * @throws IllegalStateException si l'acteur n'est pas Admin.
     */
    @Transactional
    fun deleteMessageAsAdmin(actorId: Int, messageId: Int) {
        val message = messageRepo.findById(messageId)
            .orElseThrow { IllegalArgumentException("Message not found with id $messageId") }

        val conversation = message.conversation ?: throw IllegalArgumentException("Message has no conversation")
        val conversationId = conversation.id ?: throw IllegalArgumentException("Conversation id missing")

        val actorParticipant = conversation.participants.find { it.user?.id == actorId }
            ?: throw IllegalArgumentException("Actor is not a participant in conversation $conversationId")

        if (actorParticipant.role != "Admin") {
            throw IllegalStateException("Only admins can delete others' messages")
        }

        messageRepo.delete(message)
    }

    /**
     * Supprime un message si l'utilisateur est l'expéditeur.
     * @param messageId identifiant du message.
     * @param userId identifiant de l'utilisateur demandant la suppression.
     * @throws IllegalArgumentException si le message n'existe pas ou si l'utilisateur n'est pas l'expéditeur.
     */
    fun deleteMessage(messageId: Int, userId: Int) {
        val message = messageRepo.findById(messageId)
            .orElseThrow { IllegalArgumentException("Message not found with id $messageId") }

        if (message.sender.id != userId) {
            throw IllegalArgumentException("User is not allowed to delete this message")
        }

        messageRepo.delete(message)
    }
}
