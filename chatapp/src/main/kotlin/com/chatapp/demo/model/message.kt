package com.chatapp.demo.model
import jakarta.persistence.*
import java.time.LocalDateTime


/**
 * Classe abstraite représentant un message dans une conversation.
 *
 * Cette entité est la classe de base pour les différents types de messages
 * (texte, image, vidéo, etc.), utilisant une stratégie d’héritage **JOINED**.
 *
 * @property id identifiant unique du message.
 * @property created_at date et heure de création du message.
 * @property type type de message (ex. : "text", "image", "video").
 * @property conversation conversation à laquelle le message appartient.
 * @property sender utilisateur ayant envoyé le message.
 */
@Entity
@Table(name="message")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class message(
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    var id:Int?=null,
    @Column(name="created_at")
    var created_at:LocalDateTime=LocalDateTime.now(),
    @Column(name="type")
    var type:String,

    @ManyToOne
    @JoinColumn(name="conversation_id")
    var conversation:conversation,

    @ManyToOne
    @JoinColumn(name="sender_id")
    var sender:User
)