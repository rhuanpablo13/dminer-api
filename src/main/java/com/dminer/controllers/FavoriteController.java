package com.dminer.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;

import com.dminer.dto.CommentDTO;
import com.dminer.dto.FavoriteDTO;
import com.dminer.dto.FavoriteRequestDTO;
import com.dminer.dto.PostDTO;
import com.dminer.dto.UserReductDTO;
import com.dminer.entities.Comment;
import com.dminer.entities.Favorites;
import com.dminer.entities.Post;
import com.dminer.entities.React;
import com.dminer.entities.ReactUser;
import com.dminer.entities.User;
import com.dminer.repository.FavoritesRepository;
import com.dminer.repository.ReactRepository;
import com.dminer.repository.ReactUserRepository;
import com.dminer.response.Response;
import com.dminer.services.CommentService;
import com.dminer.services.PostService;
import com.dminer.services.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/favorite")
@CrossOrigin(origins = "*")
public class FavoriteController {
    
    private static final Logger log = LoggerFactory.getLogger(FavoriteController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;
    
    @Autowired
    private FavoritesRepository favoritesRepository;

    @Autowired
	private ReactRepository reactRepository;

    @Autowired
	private ReactUserRepository reactUserRepository;

    

    private void validateRequestDto(FavoriteRequestDTO dto, BindingResult result) {
        if (dto.getLogin() == null || dto.getLogin().isBlank()) {
            result.addError(new ObjectError("FavoriteRequestDTO", "Login do usuário precisa estar preenchido."));
		} else {
            Optional<User> findById = userService.findByLogin(dto.getLogin());
            if (!findById.isPresent()) {
                result.addError(new ObjectError("FavoriteRequestDTO", "Usuário: "+ dto.getLogin() +" não encontrado."));
            }
        }

        if (dto.getIdPost() == null)  {
            result.addError(new ObjectError("dto", "Id do Post precisa estar preenchido."));
		} else {
			Optional<Post> opt = postService.findById(dto.getIdPost()); 
            if (!opt.isPresent()) {
                result.addError(new ObjectError("dto", "Post não encontrado."));
            }
        }
    }

    
    @PostMapping
    public ResponseEntity<Response<FavoriteDTO>> create(@Valid @RequestBody FavoriteRequestDTO dtoReq, BindingResult result) {
        
        log.info("Salvando um novo favorito {}", dtoReq.toString());

        Response<FavoriteDTO> response = new Response<>();

        validateRequestDto(dtoReq, result);
        if (result.hasErrors()) {
            log.info("Erro validando dtoReq: {}", dtoReq);
            result.getAllErrors().forEach( e -> response.getErrors().add(e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> user = userService.findByLogin(dtoReq.getLogin());
        Optional<Favorites> favos = favoritesRepository.findByUserAndPost(user.get(), new Post(dtoReq.getIdPost()));
        if (favos.isPresent()) {
            favoritesRepository.deleteById(favos.get().getId());
            return ResponseEntity.ok().build();
        }

        Optional<Post> opt = postService.findById(dtoReq.getIdPost()); 
        Post post = opt.get();
        
        Favorites favorite = new Favorites();
        favorite.setPost(new Post(dtoReq.getIdPost()));
        favorite.setUser(user.get());
        favorite = favoritesRepository.save(favorite);

        FavoriteDTO dto = new FavoriteDTO();
        dto.setId(favorite.getId());
        dto.setPostDto(new PostDTO(dtoReq.getIdPost()));
        dto.setLogin(dtoReq.getLogin());
        List<Favorites> favs = favoritesRepository.findAllByPost(post);
        if (favs != null) {
            dto.getPostDto().setFavorites(new ArrayList<>());
            favs.forEach(fav -> {
                dto.getPostDto().getFavorites().add(fav.getUser().getLogin());
            });
        }
        response.setData(dto);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/all-by-user/{login}")
    public ResponseEntity<Response<List<PostDTO>>> allByUser(@PathVariable String login) {
        
        log.info("Buscando todos por usuário {}", login);

        Response<List<PostDTO>> response = new Response<>();

        if (login == null || login.isBlank()) {
            response.setErrors(Arrays.asList("Informe o login do usuário"));
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> user = userService.findByLogin(login);
        List<Favorites> favoritos = favoritesRepository.findAllByUser(user.get());
        
        List<PostDTO> postsDto = new ArrayList<>();

        favoritos.forEach(fav -> {
            PostDTO postDto = fav.getPost().convertDto();
            postsDto.add(postDto);
        });

        postsDto.forEach(dtoPost -> {
            favoritos.forEach(fav -> {                
                if (fav.getPost().getId() == dtoPost.getId()) {
                    if (dtoPost.getFavorites() == null) dtoPost.setFavorites(new ArrayList<>());
                    dtoPost.getFavorites().add(fav.getUser().getLogin());
                }
            }); 
        });


        postsDto.forEach(dtoPost -> {
            Optional<List<Comment>> comment = commentService.findByPost(new Post(dtoPost.getId()));
            List<CommentDTO> comments = new ArrayList<>();
            if (comment.isPresent()) {
                comment.get().forEach(c -> {
                    comments.add(c.convertDto());
                });
            }
            dtoPost.setComments(comments);

            String avatar = userService.getAvatarEndpoint(dtoPost.getUser().getLogin());
            dtoPost.getUser().setAvatar(avatar);
            dtoPost.setReacts(getReacts(new Post(dtoPost.getId())));

        });

        response.setData(postsDto);
        return ResponseEntity.ok(response);
    }


    private Map<String, List<String>> getReacts(Post post) {
		Map<String, List<String>> dto = new HashMap<>();
		
		List<React> reacts = reactRepository.findAll();
		reacts.forEach(react -> {
			dto.put(react.getReact(), new ArrayList<String>());
		});

		List<ReactUser> reactsUsers = reactUserRepository.findByPost(post);
		
		if (reactsUsers != null && !reactsUsers.isEmpty()) {
			reactsUsers.forEach(like -> {
				String login = like.getLogin();
				String react = like.getReact().getReact();
				if (dto.containsKey(react)) {
					dto.get(react).add(login);
				} else {
					dto.put(react, Arrays.asList(login));
				}
			});
		}
		return dto;
	} 


    @GetMapping("/all-by-post/{idPost}")
    public ResponseEntity<Response<List<FavoriteDTO>>> allByPost(@PathVariable Integer idPost) {
        
        log.info("Buscando todos por post {}", idPost);

        Response<List<FavoriteDTO>> response = new Response<>();

        if (idPost == null) {
            response.setErrors(Arrays.asList("Informe o id do post"));
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Post> post = postService.findById(idPost);
        if (!post.isPresent()) {
            response.setErrors(Arrays.asList("Nenhum favorito encontrado"));
            return ResponseEntity.badRequest().body(response);
        }

        List<Favorites> favos = favoritesRepository.findAllByPost(new Post(idPost));
        if (favos.isEmpty()) {
            response.setErrors(Arrays.asList("Nenhum favorito encontrado"));
            return ResponseEntity.ok().body(response);
        }

        List<FavoriteDTO> favosDto = new ArrayList<>();
        favos.forEach(f -> {
            FavoriteDTO dto = new FavoriteDTO();
            dto.setId(f.getId());
            dto.setPostDto(f.getPost().convertDto());
            dto.setLogin(f.getUser().getLogin());
            favosDto.add(dto);
        });

        response.setData(favosDto);
        return ResponseEntity.ok(response);
    }

}