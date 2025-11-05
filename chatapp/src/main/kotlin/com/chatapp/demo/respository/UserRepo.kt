package com.chatapp.demo.respository

import com.chatapp.demo.model.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Interface du dépôt (repository) pour la gestion des entités [User].
 *
 * Cette interface gère les opérations de persistance des utilisateurs,
 * y compris la création, la mise à jour, la suppression et la recherche
 * d’utilisateurs dans la base de données.
 *
 * Elle hérite de [JpaRepository] pour bénéficier des fonctionnalités JPA standard.
 */
@Repository
interface UserRepo:JpaRepository<User,Int?> {
    /**
     * Cette fonction vérifie si un utilisateur existe par son email ou son numero de telephone.
     * @param email l'email
     * @param phone le numero de telephone
     * @return un utilisateur s’il existe, ou Optional.empty() sinon
     */
    fun findByEmailOrPhone(email: String?, phone: String?):Optional<User>

    /**
     * Cette fonction vérifie si un utilisateur existe par son username
     * @param username le numero de telephone
     * @return un utilisateur s’il existe, ou Optional.empty() sinon
     */
    fun findByUsername(username: String): Optional<User>

    /**
     * Cette fonction vérifie si un utilisateur existe par  son numero de telephone.
     * @param phone le numero de telephone
     * @return true s’il existe, ou false sinon
     */
    fun existsByPhone(phone:String):Boolean

    /**
     * Cette fonction vérifie si un utilisateur existe par  son email
     * @param email l¡email
     * @return true s’il existe, ou false sinon
     */
    fun existsByEmail(email:String):Boolean


    /**
     * Cette fonction match un query qui peut etre username,email ou numero de telephone.
     * @Param query
     * @Param pageable un parametre qui define les proprietes de pagination comme les pages et le size
     * @return une list des utilisateurs ou une list vide
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun searchByUsernameEmailOrPhone(@Param("query") query: String,  pageable: Pageable): List<User>
}

