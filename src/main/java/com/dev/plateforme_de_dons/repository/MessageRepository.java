package com.dev.plateforme_de_dons.repository;

import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.Message;
import com.dev.plateforme_de_dons.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(path = "messages", collectionResourceRel = "messages")
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findBySender(User sender, Pageable pageable);

    Page<Message> findByReceiver(User receiver, Pageable pageable);

    Page<Message> findByReceiverAndReadFalse(User receiver, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE (m.sender = :user OR m.receiver = :user) ORDER BY m.sentAt DESC")
    Page<Message> findAllByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.annonce = :annonce AND " +
           "((m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1)) " +
           "ORDER BY m.sentAt ASC")
    List<Message> findConversation(@Param("user1") User user1, @Param("user2") User user2, @Param("annonce") Annonce annonce);

    @Query("SELECT DISTINCT CASE WHEN m.sender = :user THEN m.receiver ELSE m.sender END FROM Message m " +
           "WHERE m.sender = :user OR m.receiver = :user")
    List<User> findConversationPartners(@Param("user") User user);

    long countByReceiverAndReadFalse(User receiver);

    Page<Message> findByAnnonce(Annonce annonce, Pageable pageable);
}
