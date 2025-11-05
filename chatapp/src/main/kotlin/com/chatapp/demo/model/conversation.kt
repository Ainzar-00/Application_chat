package com.chatapp.demo.model
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Entité représentant une conversation dans l’application de messagerie.
 *
 * Cette classe décrit les métadonnées d’une conversation (type, nom, date de création, etc.)
 * ainsi que ses relations avec les utilisateurs et les messages.
 *
 * Elle est mappée à la table **conversations** dans la base de données.
 *
 * @property id identifiant unique de la conversation (clé primaire).
 * @property type type de la conversation (ex. : "privée", "groupe").
 * @property name nom de la conversation (facultatif pour les conversations privées).
 * @property created_at date et heure de création de la conversation.
 * @property lastMessageAt date et heure du dernier message envoyé (mise à jour automatiquement).
 * @property created_by utilisateur ayant créé la conversation.
 * @property participants liste des participants associés à cette conversation.
 * @property messages liste des messages échangés dans cette conversation.
 */
@Entity
@Table(name="conversations")
class conversation(
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    var id:Int?=null,
    @Column(name="type")
    val type:String,
    @Column(name="name")
    var name:String?,
    @Column(name="created_at")
    val created_at:LocalDateTime=LocalDateTime.now(),
    @Column(name="lastMessageAt")
    var lastMessageAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    val created_by: User,

    @OneToMany(mappedBy = "conversation",cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true)
    val participants: MutableList<ConversationParticipant> = mutableListOf(),

    @OneToMany(mappedBy = "conversation", cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true)
    var messages: MutableList<message> = mutableListOf()

)
