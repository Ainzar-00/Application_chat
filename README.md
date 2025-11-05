# Nom de votre Projet
 wechat
 
## Description 
C’est une application de messagerie moderne qui vous permet de créer des conversations avec vos amis et de profiter d’un chat instantané 
 
## Technologies Utilisées

 - *Langage* : Kotlin  <br>- *Framework* : Spring Boot<br> - *Base de données* : MySQL<br> - *Build Tool* :  Gradle 
 
## 

 Diagramme UML 
<img width="1233" height="681" alt="Screenshot 2025-11-02 010639" src="https://github.com/user-attachments/assets/ad03466c-b5d5-408e-88b5-27a3897ac476" />

 
## Structure de la Base de Données

  - *User* : Cette entité décrit les attributs et les caractéristiques de l’utilisateur <br>
 - *Conversation* : Cette entité représente une conversation. Les types de conversation sont : privé ou de groupe. L’attribut lastMessage indique si la conversation contient des messages : s’il y en a, elle est classée comme chat, sinon comme contact. <br>
 - *conversation_particpants* : Cette entité contient les participants d’une conversation et indique le rôle et le statut de chaque utilisateur<br>
 -  *Message* : Cette entité est abstraite. Elle décrit une conversation pour chaque message envoyé, qui l’a envoyé, et le type de message.<br>
 -  *Message_Text* : Cette entité hérite de l’entité Message et contient uniquement l’attribut content pour le texte du message.<br>
 
## 

 Installation et Exécution 
 
### Prérequis - JDK 17+ - MySQL installé - Gradle 
 
 
### Étapes d'installation 
1. Clonez le repository 
bash 
   git clone https://github.com/Ainzar-00/Application-chat.git 
 
 
2. Créez la base de données 
sql 
   CREATE DATABASE ch01; 
 
 
3. Configurez application.yml 
properties 
   
server:
  port: 8080

  servlet:
    session:
      cookie:
        same-site: none
        secure: true
spring:
  security:
    user:
      name: "admin"
      password: "0"

  datasource:
    url: jdbc:mysql://localhost:3309/ch01
    username: root
    password: ${DB_PASSWORD}

    #url: jdbc:h2:mem:testdb
    #driverClassName: org.h2.Driver
    #username: sa
    #password: "4"
  h2:
      console:
        enabled: true
        path: /h2-console

  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
#        dialect: org.hibernate.dialect.MySQL8Dialect
 
 
4. Lancez l'application 
bash 
   ./gradlew bootRun
 
 
## Endpoints Disponibles 

 
### User 

POST	/api/users/signUp	Create a new user (sign up).	Public (no authentication) <br>
POST	/api/users/login	Login user and create session.	Public<br>
GET	/api/users/current	Get the currently logged-in user.	Authenticated user<br>
POST	/api/users/logout	Logout the current user (invalidate session).	Authenticated user<br>
GET	/api/users/{id}	Get a specific user by ID.	Public (anyone can query by ID)<br>
PUT	/api/users/username	Update logged-in user’s username.	Authenticated user<br>
PUT	/api/users/email	Update logged-in user’s email.	Authenticated user<br>
PUT	/api/users/phone	Update logged-in user’s phone number.	Authenticated user<br>
PUT	/api/users/password	Update logged-in user’s password. Must match confirmation.	Authenticated user<br>
DELETE	/api/users/{id}	Delete a user by ID.	Public (no role check here, but you might want to restrict to admin)<br>

### Conversation 
POST	/api/conversations/private	Create a private conversation with another user.	Authenticated user<br>
POST	/api/conversations/group	Create a group conversation.	Authenticated user<br>
GET	/api/conversations/{id}	Get a conversation by its ID.	Public (no session check)<br>
GET	/api/conversations/search/users	Search users by query string.	Public<br>
GET	/api/conversations/search/groups	Search groups by query string.	Public<br>
GET	/api/conversations/user	Get all conversations of the logged-in user.	Authenticated user<br>
GET	/api/conversations/chats	Get all chat conversations (with messages) of the logged-in user.	Authenticated user<br>
GET	/api/conversations/contacts	Get all contact conversations (without messages) of the logged-in user.	Authenticated user<br>
PATCH	/api/conversations/participants/block	Block a user in a conversation.	Authenticated user<br>
PATCH	/api/conversations/participants/deblock	Unblock a user in a conversation.	Authenticated user<br>
PUT	/api/conversations/{id}/name	Update a group conversation’s name.	Authenticated user<br>
DELETE	/api/conversations/participant	Remove a participant from a conversation.	Authenticated user<br>

### Conversation_Particpants
POST	/api/conversations/{conversationId}/participants	Add a participant to a conversation.	Authenticated user<br>
GET	/api/conversations/{conversationId}/participants	Get all participants of a conversation.	Public (no session check)<br>
DELETE	/api/conversations/{conversationId}/participants/{targetUserId}	Remove a participant from a conversation.	Authenticated user<br>
### Message
POST	/api/conversations/{conversationId}/messages	Send a message in a conversation.	Authenticated user<br>
GET	/api/conversations/{conversationId}/messages	Get all messages in a conversation.	Public (no session check)<br>
DELETE	/api/conversations/{conversationId}/messages/{messageId}	Delete a message sent by the logged-in user.	Authenticated user<br>
DELETE	/api/conversations/{conversationId}/messages/{messageId}/admin	Delete a message as an admin.	Authenticated user (admin action implied)<br>

## Auteur 

*Youness Ainzar* - Projet Back-End Kotlin/Spring Boot 
 
## Date 
1 November 2025
