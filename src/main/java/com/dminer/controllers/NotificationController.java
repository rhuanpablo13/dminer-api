package com.dminer.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import com.dminer.converters.NotificationConverter;
import com.dminer.dto.NotificationDTO;
import com.dminer.dto.NotificationRequestDTO;
import com.dminer.entities.Notification;
import com.dminer.entities.User;
import com.dminer.response.Response;
import com.dminer.services.NotificationService;
import com.dminer.services.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notification")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired 
    private NotificationConverter notificationConverter;

    @Autowired
    private UserService userService;

    @Autowired
    private ServerSendEvents sendEvents;

    private void validateRequestDto(NotificationRequestDTO notificationRequestDTO, BindingResult result) {
        if (notificationRequestDTO.getIdUser() == null) {
            result.addError(new ObjectError("NotificationRequestDTO", "Id do usuário precisa estar preenchido."));
		} else {
            Optional<User> findById = userService.findById(notificationRequestDTO.getIdUser());
            if (!findById.isPresent()) {
                result.addError(new ObjectError("NotificationRequestDTO", "Usuário não encontrado."));
            }
        }

        if (notificationRequestDTO.getNotification() == null || notificationRequestDTO.getNotification().isEmpty()) {
            result.addError(new ObjectError("NotificationRequestDTO", "Descrição da notificação precisa estar preenchido."));			
		}
    }
    

    private void validateDto(NotificationDTO dto, BindingResult result) {
        if (dto.getId() == null) {
            result.addError(new ObjectError("dto", "Id da notificação precisa estar preenchido."));
		} else {
            Optional<Notification> findById = notificationService.findById(dto.getIdUser());
            if (!findById.isPresent()) {
                result.addError(new ObjectError("dto", "Notificação não encontrada."));
            }
        }
        
        if (dto.getIdUser() == null) {
            result.addError(new ObjectError("dto", "Id do usuário precisa estar preenchido."));
		} else {
            Optional<User> findById = userService.findById(dto.getIdUser());
            if (!findById.isPresent()) {
                result.addError(new ObjectError("dto", "Usuário não encontrado."));
            }
        }

        if (dto.getNotification() == null || dto.getNotification().isEmpty()) {
            result.addError(new ObjectError("dto", "Descrição da notificação precisa estar preenchido."));			
		}
    }

    @PostMapping
    public ResponseEntity<Response<NotificationDTO>> create(@Valid @RequestBody NotificationRequestDTO notificationRequest, BindingResult result) {
    
        log.info("Salvando uma nova notificação {}", notificationRequest);

		Response<NotificationDTO> response = new Response<>();
        validateRequestDto(notificationRequest, result);
        if (result.hasErrors()) {
            log.info("Erro validando notificationRequest: {}", notificationRequest);
            result.getAllErrors().forEach( e -> response.getErrors().add(e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(response);
        }
        
        Notification events = notificationService.persist(notificationConverter.requestDtoToEntity(notificationRequest));
        response.setData(notificationConverter.entityToDto(events));
        sendEvents.streamSseNotification();
        return ResponseEntity.ok().body(response);
    }


    @PutMapping()
    public ResponseEntity<Response<NotificationDTO>> put( @Valid @RequestBody NotificationDTO dto, BindingResult result) {

        log.info("Alterando um notification {}", dto);

        Response<NotificationDTO> response = new Response<>();

        validateDto(dto, result);
        if (result.hasErrors()) {
            log.info("Erro validando NotificationDTO: {}", dto);
            result.getAllErrors().forEach( e -> response.getErrors().add(e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(response);
        }

        Notification notification = notificationService.persist(notificationConverter.dtoToEntity(dto));
        response.setData(notificationConverter.entityToDto(notification));
        return ResponseEntity.ok().body(response);
    }

    
    @GetMapping(value = "/find/{id}")
    public ResponseEntity<Response<NotificationDTO>> get(@PathVariable("id") Integer id) {
        log.info("Buscando notificação {}", id);
        
        Response<NotificationDTO> response = new Response<>();
        if (id == null) {
            response.getErrors().add("Informe um id");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Notification> user = notificationService.findById(id);
        if (!user.isPresent()) {
            response.getErrors().add("Notificação não encontrada");
            return ResponseEntity.status(404).body(response);
        }

        response.setData(notificationConverter.entityToDto(user.get()));
        return ResponseEntity.ok().body(response);
    }


    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Response<NotificationDTO>> delete(@PathVariable("id") Integer id) {
        log.info("Buscando notificação {}", id);
        
        Response<NotificationDTO> response = new Response<>();
        if (id == null) {
            response.getErrors().add("Informe um id");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Notification> not = notificationService.findById(id);
        if (!not.isPresent()) {
            response.getErrors().add("Notificação não encontrada");
            return ResponseEntity.status(404).body(response);
        }

        try {notificationService.delete(id);}
        catch (EmptyResultDataAccessException e) {
            response.getErrors().add("Notificação não encontrado");
            return ResponseEntity.status(404).body(response);
        }

        response.setData(notificationConverter.entityToDto(not.get()));
        return ResponseEntity.ok().body(response);
    }


    @GetMapping("/all")
    public ResponseEntity<Response<List<NotificationDTO>>> getAll() {
        
        Response<List<NotificationDTO>> response = new Response<>();

        Optional<List<Notification>> user = notificationService.findAll();
        if (user.get().isEmpty()) {
            response.getErrors().add("Eventos não encontrados");
            return ResponseEntity.status(404).body(response);
        }

        List<NotificationDTO> eventos = new ArrayList<>();
        user.get().forEach(u -> {
            eventos.add(notificationConverter.entityToDto(u));
        });
        response.setData(eventos);
        return ResponseEntity.ok().body(response);
    }
}
