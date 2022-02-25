package com.dminer.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.dminer.converters.TutorialsConverter;
import com.dminer.dto.TutorialsRequestDTO;
import com.dminer.dto.TutorialsDTO;
import com.dminer.entities.Tutorials;
import com.dminer.repository.CategoryRepository;
import com.dminer.repository.GenericRepositoryPostgres;
import com.dminer.repository.GenericRepositorySqlServer;
import com.dminer.repository.PermissionRepository;
import com.dminer.repository.TutorialsRepository;
import com.dminer.response.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tutorials")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TutorialsController {
    
    private static final Logger log = LoggerFactory.getLogger(TutorialsController.class);

    @Autowired 
    private TutorialsConverter tutorialsConverter;

    @Autowired
    private TutorialsRepository tutorialsRepository;

    @Autowired
    GenericRepositorySqlServer tutorialsRepositorySqlServer;

    @Autowired
    GenericRepositoryPostgres tutorialsRepositoryPostgres;

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private Environment env;


    private void validateRequestDto(TutorialsRequestDTO dto, BindingResult result) {
        if (dto.getTitle() == null || dto.getTitle().isEmpty()) {
            result.addError(new ObjectError("TutorialsRequestDTO", "Title precisa estar preenchido."));
        }

        if (dto.getContent() == null || dto.getContent().isEmpty()) {
            result.addError(new ObjectError("TutorialsRequestDTO", "Conteúdo precisa estar preenchido."));
        }

        if (dto.getPermission() == null) {
            dto.setPermission("");
        } 

        if (dto.getCategory() == null) {
            result.addError(new ObjectError("TutorialsRequestDTO", "Categoria precisa estar preenchido."));
		} else {
            // if(!categoryRepository.existsByTitle(dto.getCategory())) {
            //     result.addError(new ObjectError("dto", "Categoria não é válida."));
            // }
            if(!categoryRepository.existsById(dto.getCategory())) {
                result.addError(new ObjectError("dto", "Categoria não é válida."));
            }
        }

        if (dto.getDate() == null || dto.getTitle().isEmpty()) {
            result.addError(new ObjectError("TutorialsRequestDTO", "Data precisa estar preenchida no formato yyyy-mm-dd hh:mm:ss"));
        }
       
    }
    

    private void validateDto(TutorialsDTO dto, BindingResult result) {
        if (dto.getTitle() == null || dto.getTitle().isEmpty()) {
            result.addError(new ObjectError("dto", "Title precisa estar preenchido."));
        }

        if (dto.getContent() == null || dto.getContent().isEmpty()) {
            result.addError(new ObjectError("dto", "Conteúdo precisa estar preenchido."));
        }

        if (dto.getId() == null) {
            dto.setPermission("");
        }

        if (dto.getCategory() == null) {
            result.addError(new ObjectError("dto", "Categoria precisa estar preenchido."));
		} else {
            if(!categoryRepository.existsByName(dto.getCategory())) {
                result.addError(new ObjectError("dto", "Categoria não é válida."));
            }
        }

        if (dto.getDate() == null || dto.getTitle().isEmpty()) {
            result.addError(new ObjectError("dto", "Data precisa estar preenchida no formato yyyy-mm-dd hh:mm:ss"));
        }
       
    }
   

    @PostMapping
    public ResponseEntity<Response<TutorialsDTO>> create(@Valid @RequestBody TutorialsRequestDTO dto, BindingResult result) {
    
		Response<TutorialsDTO> response = new Response<>();
        validateRequestDto(dto, result);
        if (result.hasErrors()) {
            log.info("Erro validando dto: {}", dto);
            result.getAllErrors().forEach( e -> response.getErrors().add(e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(response);
        }
        
        Tutorials doc = tutorialsRepository.save(tutorialsConverter.requestDtoToEntity(dto));
        response.setData(tutorialsConverter.entityToDTO(doc));

        return ResponseEntity.ok().body(response);
    }

    @PutMapping()
    public ResponseEntity<Response<TutorialsDTO>> put( @Valid @RequestBody TutorialsDTO dto, BindingResult result) {

        log.info("Alterando um categoria {}", dto);

        Response<TutorialsDTO> response = new Response<>();

        validateDto(dto, result);
        
        Tutorials benefits = tutorialsRepository.save(tutorialsConverter.dtoToEntity(dto));
        response.setData(tutorialsConverter.entityToDTO(benefits));
        return ResponseEntity.ok().body(response);
    }

    
    @GetMapping(value = "/find/{id}")
    public ResponseEntity<Response<TutorialsDTO>> get(@PathVariable("id") Integer id) {
        
        Response<TutorialsDTO> response = new Response<>();
        if (id == null) {
            response.getErrors().add("Informe um id");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Tutorials> doc = tutorialsRepository.findById(id);
        if (!doc.isPresent()) {
            response.getErrors().add("Tutorial não encontrado");
            return ResponseEntity.ok().body(response);
        }

        response.setData(tutorialsConverter.entityToDTO(doc.get()));
        return ResponseEntity.ok().body(response);
    }

    @GetMapping(value = "/search/{search}")
    public ResponseEntity<Response<TutorialsDTO>> search(@PathVariable("search") String search) {
        
        Response<TutorialsDTO> response = new Response<>();
        if (search == null) {
            response.getErrors().add("Informe algo para pesquisar");
            return ResponseEntity.badRequest().body(response);
        }

        List<Tutorials> search2 = new ArrayList<>();

        if (isProd()) {
            search2 = tutorialsRepositoryPostgres.searchTutorial(search);            
        } else {
            search2 = tutorialsRepositorySqlServer.searchTutorials(search);
        }

        if (search2.isEmpty()) {
            response.getErrors().add("Nenhum dado encontrado");
            return ResponseEntity.ok().body(response);
        }
        
        search2.forEach(s -> {
            response.setData(tutorialsConverter.entityToDTO(s));
        });
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Response<TutorialsDTO>> delete(@PathVariable("id") Integer id) {
        
        Response<TutorialsDTO> response = new Response<>();
        if (id == null) {
            response.getErrors().add("Informe um id");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Tutorials> doc = tutorialsRepository.findById(id);
        if (!doc.isPresent()) {
            response.getErrors().add("Tutorial não encontrado");
            return ResponseEntity.ok().body(response);
        }

        try {tutorialsRepository.deleteById(id);}
        catch (EmptyResultDataAccessException e) {
            response.getErrors().add("Tutorial não encontrado");
            return ResponseEntity.ok().body(response);
        }

        response.setData(tutorialsConverter.entityToDTO(doc.get()));
        return ResponseEntity.ok().body(response);
    }


    @GetMapping("/all")
    public ResponseEntity<Response<List<TutorialsDTO>>> getAll(@RequestHeader("x-access-adminUser") String perfil) {
        
        Response<List<TutorialsDTO>> response = new Response<>();

        List<Tutorials> doc = tutorialsRepository.findAllByOrderByDateDesc();

        if (doc.isEmpty()) {
            response.getErrors().add("Tutorials não encontrados");
            return ResponseEntity.ok().body(response);
        }

        List<TutorialsDTO> eventos = new ArrayList<>();

        if (! perfil.equalsIgnoreCase("1")) {
            doc = doc.stream().filter(d -> d.getPermission().equalsIgnoreCase("0")).collect(Collectors.toList());
        }

        doc.forEach(u -> {
            eventos.add(tutorialsConverter.entityToDTO(u));
        });

        response.setData(eventos);
        return ResponseEntity.ok().body(response);
    }


    public boolean isProd() {
        return Arrays.asList(env.getActiveProfiles()).contains("prod");
    }
}
