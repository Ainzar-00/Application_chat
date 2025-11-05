package com.chatapp.demo.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Clé composite représentant l’association entre un utilisateur et une conversation.
 *
 * Cette classe sert de clé primaire pour l’entité [ConversationParticipant].
 * Elle combine l’ID de la conversation et celui de l’utilisateur afin d’assurer
 * l’unicité de chaque participation.
 *
 * @property conversationId identifiant de la conversation.
 * @property userId identifiant de l’utilisateur.
 */
@Embeddable
data class ConversationParticipantId(
    @Column(name = "conversation_id")
    var conversationId: Int = 0,

    @Column(name = "user_id")
    var userId: Int = 0
) : java.io.Serializable


/**
 * Entité représentant un participant dans une conversation.
 *
 * Chaque enregistrement de cette table relie un utilisateur à une conversation,
 * avec des informations supplémentaires comme le rôle, le statut et la date d’adhésion.
 *
 * Elle utilise une clé composite [ConversationParticipantId].
 *
 * @property id clé composite contenant les identifiants de la conversation et de l’utilisateur.
 * @property conversation conversation à laquelle le participant est associé.
 * @property user utilisateur participant à la conversation.
 * @property role rôle du participant (ex. : "admin", "membre").
 * @property status statut du participant (ex. : "actif", "quitté").
 * @property joinat date et heure de l’adhésion à la conversation.
 * @property customName nom personnalisé du participant dans cette conversation (optionnel).
 */

@Entity
@Table(name = "conversation_participants")
open class ConversationParticipant(
    @EmbeddedId
    open var id: ConversationParticipantId = ConversationParticipantId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversationId")
    @JoinColumn(name = "conversation_id")
open var conversation: conversation? = null,

     @ManyToOne(fetch = FetchType.LAZY)
     @MapsId("userId")
     @JoinColumn(name = "user_id")
open var user: User? = null,

    @Column(name="role")
    var role:String?,

    @Column(name="status")
    var status:String,

    @Column(name="join_at")
    var joinat: LocalDateTime= LocalDateTime.now(),

    @Column(name = "custom_name")
    var customName: String? = null
)