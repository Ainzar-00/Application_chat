package com.chatapp.demo.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Entité représentant un utilisateur de l’application.
 *
 * Cette classe contient les informations principales d’un utilisateur,
 * y compris ses identifiants, ses coordonnées et la liste des messages qu’il a envoyés.
 *
 * Elle est mappée à la table **users** dans la base de données.
 *
 * @property id identifiant unique de l’utilisateur (clé primaire).
 * @property username nom d’utilisateur (doit être unique dans le système).
 * @property email adresse e-mail de l’utilisateur.
 * @property phone numéro de téléphone de l’utilisateur.
 * @property created_at date et heure de création du compte (générée automatiquement).
 * @property password mot de passe chiffré de l’utilisateur.
 * @property profile_image chemin ou URL de l’image de profil (facultatif).
 * @property messages liste des messages envoyés par l’utilisateur (relation un-à-plusieurs).
 */
@Entity
@Table(name="users")
class User(
           @Id
           @GeneratedValue(strategy = GenerationType.IDENTITY)
           var id: Int?=null,
           @Column(name="username", nullable = false)
           var username:String,
           @Column(name="email", nullable = false)
           var email:String,
           @Column(name="phone", nullable = false)
           var phone:String,
           @Column(name="created_at", nullable = false)
           val created_at:LocalDateTime=LocalDateTime.now(),
           @Column(name="password", nullable = false)
           var password:String,
           @Column(name="profile_image", nullable = true)
           var profile_image:String?=null,

           @OneToMany(mappedBy="sender")
           var messages:MutableList<message> =mutableListOf()
)
