package com.chatapp.demo.service

import com.chatapp.demo.model.User
import com.chatapp.demo.respository.UserRepo
import org.springframework.stereotype.Service
import com.chatapp.demo.service.ValidationHelpers
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.io.File
import java.io.FileNotFoundException

/**
 * Exception levée lorsqu'une ressource dupliquée est détectée (ex : email ou téléphone déjà enregistré).
 * Cette exception renvoie le statut HTTP 409 (CONFLICT) grâce à l'annotation [ResponseStatus].
 * @param message message décrivant le conflit.
 */
@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateResourceException(message: String) : RuntimeException(message)

/**
 * Service de gestion des utilisateurs.
 * Ce service encapsule la logique métier relative à l'authentification, la création,
 * la mise à jour et la suppression des utilisateurs.
 * @property usereRepo dépôt d'accès aux utilisateurs.
 * @property passwordencoder encodeur de mot de passe fourni par Spring Security.
 */
@Service
class UserService(private val usereRepo: UserRepo, private val passwordencoder:PasswordEncoder){

    /**
     * Authentifie un utilisateur par e-mail ou par téléphone et vérifie le mot de passe.
     * @param email adresse e-mail (nullable) — tu peux utiliser soit email soit phone.
     * @param phone numéro de téléphone (nullable) — tu peux utiliser soit phone soit email.
     * @param password mot de passe en clair fourni par l’utilisateur.
     * @return l'entité [User] correspondante si les informations sont valides.
     * @throws IllegalArgumentException si ni email ni phone ne sont fournis, si l'utilisateur n'existe pas ou si le mot de passe est incorrect.
     */
    fun logIn(email:String?,phone:String?,password:String):User {
        if(email==null && phone==null){
            throw IllegalArgumentException("You should log in using either your phone number or your email")
        }
        val user=usereRepo.findByEmailOrPhone(email,phone)
            .orElseThrow{ IllegalArgumentException("No user found with this Phone number or Email")}

        if(!passwordencoder.matches(password, user.password)){
            throw IllegalArgumentException("Incorrect password")
        }
        return user

    }

    /**
     * Inscrit un nouvel utilisateur après validation des champs.
     * Validations réalisées :
     * - nom d'utilisateur via `ValidationHelpers.generalHelpers.validatename`
     * - email via `ValidationHelpers.FieldHelper.validateEmail`
     * - téléphone via `ValidationHelpers.FieldHelper.validatePhone`
     * - mot de passe via `ValidationHelpers.FieldHelper.validatePassword`
     * @param username nom d'utilisateur (par défaut ".") — sera validé.
     * @param email adresse e-mail (doit être unique).
     * @param phone numéro de téléphone (doit être unique).
     * @param password mot de passe en clair (sera encodé avant sauvegarde).
     * @return l'entité [User] persistée.
     * @throws DuplicateResourceException si l'email ou le téléphone existent déjà (géré via DataIntegrityViolationException).
     * @throws IllegalArgumentException si une validation échoue (déléguée aux helpers).
     */
    fun signUp(username:String=".",email:String,phone:String,password:String):User{
        var username= ValidationHelpers.generalHelpers.validatename(username)
        ValidationHelpers.FieldHelper.validateEmail(email){usereRepo.existsByEmail(email)}
        ValidationHelpers.FieldHelper.validatePhone(phone){usereRepo.existsByPhone(phone)}
        ValidationHelpers.FieldHelper.validatePassword(password)
        val user= User(username=username,email=email,phone=phone,password=passwordencoder.encode(password))

        try {
            return usereRepo.save(user)
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateResourceException("Phone number Or email already exists ")
        }
    }


    /**
     * Récupère un utilisateur par son identifiant.
     * @param id identifiant utilisateur.
     * @return l'entité [User] correspondante.
     * @throws IllegalArgumentException si aucun utilisateur n'est trouvé.
     */
    fun finduserById(id:Int):User{
        return usereRepo.findById(id).orElseThrow{
            IllegalArgumentException("no user found")
        }
    }

    /**
     * Récupère un utilisateur par e-mail ou téléphone.
     * @param email e-mail recherché.
     * @param phone téléphone recherché (nullable).
     * @return l'entité [User] correspondante.
     * @throws IllegalArgumentException si aucun utilisateur n'est trouvé.
     */
    fun findUserByEmailOrPhone(email: String,phone:String?): User {
        return usereRepo.findByEmailOrPhone(email,phone).orElseThrow(

        )
    }

    /**
     * Récupère un utilisateur par son nom d'utilisateur.
     *
     * @param username nom d'utilisateur recherché.
     * @return l'entité [User] correspondante.
     * @throws IllegalArgumentException si aucun utilisateur n'est trouvé.
     */
    fun findByUsername(username:String):User{
        return usereRepo.findByUsername(username).orElseThrow(

        )
    }

    /**
     * Récupère tous les utilisateurs.
     * @return liste mutable contenant tous les [User]s.
     */
    fun getAllusers():MutableList<User>{
        return usereRepo.findAll()
    }

    /**
     * Met à jour le nom d'utilisateur d'un utilisateur existant.
     * @param userId identifiant de l'utilisateur.
     * @param newusername nouveau nom d'utilisateur (sera validé).
     * @return l'entité [User] mise à jour.
     * @throws IllegalArgumentException si l'utilisateur n'est pas trouvé.
     */
    fun updateUsername(userId:Int,newusername:String):User{
        ValidationHelpers.generalHelpers.validatename(newusername)
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.username=newusername
        return usereRepo.save(user)
    }

    /**
     * Met à jour l'email d'un utilisateur.
     * @param userId identifiant de l'utilisateur.
     * @param newemail nouvel email (validé pour unicité).
     * @return l'entité [User] mise à jour.
     * @throws IllegalArgumentException si l'utilisateur n'est pas trouvé.
     */
    fun updateEmail(userId:Int,newemail:String):User{

        ValidationHelpers.FieldHelper.validateEmail(newemail){usereRepo.existsByEmail(newemail)}
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.email=newemail
        return usereRepo.save(user)
    }

    /**
     * Met à jour le numéro de téléphone d'un utilisateur.
     * @param userId identifiant de l'utilisateur.
     * @param newPhoneNumber nouveau numéro (validé pour unicité/format).
     * @return l'entité [User] mise à jour.
     * @throws IllegalArgumentException si l'utilisateur n'est pas trouvé.
     */
    fun updatePhone(userId:Int,newPhoneNumber:String):User{
        ValidationHelpers.FieldHelper.validatePhone(newPhoneNumber){usereRepo.existsByPhone(newPhoneNumber)}

        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.phone=newPhoneNumber
        return usereRepo.save(user)
    }

    /**
     * Met à jour le mot de passe d'un utilisateur.
     *
     * @param userId identifiant de l'utilisateur.
     * @param newPassword nouveau mot de passe en clair (sera validé et encodé).
     * @return l'entité [User] mise à jour.
     * @throws IllegalArgumentException si l'utilisateur n'est pas trouvé.
     */
    fun updatePassword(userId:Int,newPassword:String):User{
        ValidationHelpers.FieldHelper.validatePassword(newPassword)
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.password= passwordencoder.encode(newPassword)
        return usereRepo.save(user)
    }

    /**
     * Met à jour le chemin de l'image de profil d'un utilisateur.
     * Vérifie que le fichier existe sur le système de fichiers avant la mise à jour.
     * @param userId identifiant de l'utilisateur.
     * @param imagePath chemin vers l'image (sur le disque).
     * @return l'entité [User] mise à jour.
     * @throws FileNotFoundException si le fichier image n'existe pas.
     * @throws IllegalArgumentException si l'utilisateur n'est pas trouvé.
     */

    fun updateProfileimage(userId:Int,imagePath:String):User{
        val file = File(imagePath)
        if(!file.exists() || !file.isFile){
            throw FileNotFoundException("File not found at path: $imagePath")
        }
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.profile_image=imagePath
        return usereRepo.save(user)
    }

    /**
     * Supprime un utilisateur.
     *
     * @param userId identifiant de l'utilisateur à supprimer.
     * @return `true` si la suppression a été effectuée.
     * @throws IllegalArgumentException si l'utilisateur n'est pas trouvé.
     */
    fun deleteUser(userId:Int):Boolean{
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        usereRepo.delete(user)
        return true
    }

    /**
     * Supprime l'image de profil d'un utilisateur (met à null le champ profile_image).
     * @param userId identifiant de l'utilisateur.
     * @return l'entité [User] mise à jour.
     * @throws IllegalArgumentException si l'utilisateur n'est pas trouvé.
     */
    fun deleteProfileimage(userId:Int):User{
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.profile_image=null
        return usereRepo.save(user)
    }
}