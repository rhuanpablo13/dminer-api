package com.dminer.converters;

import java.util.Base64;
import java.util.Optional;

import com.dminer.dto.CommentDTO;
import com.dminer.dto.CommentRequestDTO;
import com.dminer.dto.UserReductDTO;
import com.dminer.entities.Comment;
import com.dminer.entities.Post;
import com.dminer.entities.User;
import com.dminer.services.PostService;
import com.dminer.services.UserService;
import com.dminer.utils.UtilDataHora;
import com.dminer.utils.UtilNumbers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.ObjectError;

@Service
public class CommentConverter {

    @Autowired
    private UserService userService;

    @Autowired
    private UserConverter userConverter;

    @Autowired
    private PostService postService;

    private String token = null;

    
    public CommentDTO entityToDTO(Integer idPost, UserReductDTO user, Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setContent(comment.getContent() != null ? comment.getContent() : "");
        dto.setDate(comment.getTimestamp() != null ? UtilDataHora.dateToStringUTC(comment.getTimestamp()) : null);
        dto.setId(comment.getId());
        dto.setIdPost(idPost);
        dto.setUser(user);
        return dto;
    }


    public CommentDTO entityToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setContent(comment.getContent() != null ? comment.getContent() : "");
        dto.setDate(comment.getTimestamp() != null ? UtilDataHora.dateToStringUTC(comment.getTimestamp()) : null);
        dto.setId(comment.getId());

        if (comment.getPost() != null) {
        	Optional<Post> opt = postService.findById(comment.getPost().getId()); 
        	dto.setIdPost(opt.get().getId());
        }

        UserReductDTO user = userConverter.entityToUserReductDTO(comment.getUser());
        dto.setUser(user);
        return dto;
    }

    public Comment dtoToEntity(CommentDTO commentDTO, User user, Post post) {
        Comment c = new Comment();
        c.setId(UtilNumbers.isNumeric(commentDTO.getId()+"") ? commentDTO.getId() : null);
        c.setContent(commentDTO.getContent() != null ? commentDTO.getContent() : "");
        c.setTimestamp(commentDTO.getDate() != null ? UtilDataHora.toTimestamp(commentDTO.getDate()) : null);
        c.setPost(post);
        c.setUser(user);
        return c;
    }


    public Comment dtoToEntity(CommentDTO commentDTO) {
        Comment c = new Comment();
        c.setId(UtilNumbers.isNumeric(commentDTO.getId()+"") ? commentDTO.getId() : null);
        c.setContent(commentDTO.getContent() != null ? commentDTO.getContent() : "");
        c.setTimestamp(commentDTO.getDate() != null ? UtilDataHora.toTimestamp(commentDTO.getDate()) : null);

        Optional<User> user = userService.findByLogin(commentDTO.getUser().getLogin());
        if (user.isPresent()) {
            c.setUser(user.get());
        }
        
        Optional<Post> post = postService.findById(user.get().getId());
        if (post.isPresent()) {
            c.setPost(post.get());
        }
        return c;
    }


    public Comment requestDtoToEntity(CommentRequestDTO commentRequestDTO, User user, Post post) {
        Comment c = new Comment();
        c.setContent(commentRequestDTO.getContent() != null ? commentRequestDTO.getContent() : "");
        c.setTimestamp(commentRequestDTO.getDate() != null ? UtilDataHora.toTimestamp(commentRequestDTO.getDate()) : null);
        c.setUser(user);
        c.setPost(post);
        return c;
    }

    

    public Comment requestDtoToEntity(CommentRequestDTO commentRequestDTO) {
        Comment c = new Comment();
        c.setContent(commentRequestDTO.getContent() != null ? commentRequestDTO.getContent() : "");
        c.setTimestamp(commentRequestDTO.getDate() != null ? UtilDataHora.toTimestamp(commentRequestDTO.getDate()) : null);
        Optional<User> user = userService.findByLogin(commentRequestDTO.getLogin());
        if (user.isPresent()) {
            c.setUser(user.get());
        }
        
        Optional<Post> post = postService.findById(commentRequestDTO.getIdPost());
        if (post.isPresent()) {
            c.setPost(post.get());
        }
        return c;
    }
}
