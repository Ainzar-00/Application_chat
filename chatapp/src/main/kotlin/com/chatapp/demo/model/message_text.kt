package com.chatapp.demo.model
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Entité représentant un message textuel dans une conversation.
 *
 * Hérite de la classe abstraite [message] et ajoute un contenu textuel.
 *
 * @property content contenu du message.
 */
@Entity
@Table(name="message_text")
class message_text(
    @Column(name="content")
    var content:String,
    created_at:LocalDateTime=LocalDateTime.now(),
    type:String="text",
    conversation:conversation,
    sender: User
):message(id=null,created_at,type,conversation,sender)