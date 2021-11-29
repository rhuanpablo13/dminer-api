package com.dminer.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.dminer.dto.UserDTO;
import com.dminer.entities.User;
import com.dminer.repository.GenericRepositoryPostgres;
import com.dminer.repository.GenericRepositorySqlServer;
import com.dminer.repository.UserRepository;
import com.dminer.response.Response;
import com.dminer.services.interfaces.IUserService;

@Service
public class UserService implements IUserService {

    @Autowired
	private UserRepository userRepository;	
	
    @Autowired
	private GenericRepositorySqlServer genericRepositorySqlServer;

    @Autowired
	private GenericRepositoryPostgres genericRepositoryPostgres;


	private static final Logger log = LoggerFactory.getLogger(UserService.class);


    @Override
    public User persist(User user) {
        log.info("Persistindo usuário: {}", user);
		return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(int id) {
        log.info("Buscando um usuário pelo id {}", id);
		return userRepository.findById(id);
    }

    @Override
    public Optional<List<User>> findAll() {
        log.info("Buscando todos os usuários");
		return Optional.ofNullable(userRepository.findAll());
    }

    @Override
    public void delete(int id) throws EmptyResultDataAccessException {
        log.info("Excluindo um usuário pelo id {}", id);
		userRepository.deleteById(id);
    }


    public Optional<List<UserDTO>> getBirthDaysOfMonth() {
        log.info("Buscando todos os usuários que fazem aniversário no mês");
		return Optional.ofNullable(genericRepositorySqlServer.getBirthDaysOfMonth());
    }
    
    public Optional<List<UserDTO>> getBirthDaysOfMonthPostgres() {
        log.info("[Postgres] Buscando todos os usuários que fazem aniversário no mês");
		return Optional.ofNullable(genericRepositoryPostgres.getBirthDaysOfMonth());
    } 
     
    
    public boolean existsByLogin(String login) {
        log.info("Verificando se usuário existe pelo login, {}", login);
        return userRepository.findByLogin(login) != null;
    }

    public Optional<User> findByLogin(String login) {
        log.info("Recuperando usuário pelo login, {}", login);
        return Optional.ofNullable(userRepository.findByLogin(login));
    }
    
    
    public String getToken() {
    	String uri = "https://www.dminerweb.com.br:8553/api/auth/login";
    	RestTemplate restTemplate = new RestTemplate();
    	HttpHeaders headers = new HttpHeaders();    	
    	headers.setContentType(MediaType.APPLICATION_JSON);
    	JSONObject personJsonObject = new JSONObject();
        personJsonObject.put("userName", "matheus.ribeiro1");
        personJsonObject.put("userPassword", "#Matheus97");
        HttpEntity<String> request = new HttpEntity<String>(personJsonObject.toString(), headers);
        
        String personResultAsJsonStr = restTemplate.postForObject(uri, request, String.class);
        JSONObject retorno = new JSONObject(personResultAsJsonStr);
        return (String) retorno.get("baererAuthentication");
    }
    
    
    public Response<List<User>> carregarUsuariosApi(String token) {
        log.info("Recuperando todos os usuário na api externa");

        String uri = "https://www.dminerweb.com.br:8553/api/administrative/client_area/user/select_user";
        List<User> usuarios = new ArrayList<>();        
    	RestTemplate restTemplate = new RestTemplate();
    	Response<List<User>> myresponse = new Response<>();
    	HttpHeaders headers = new HttpHeaders();
    	headers.add("BAERER_AUTHENTICATION", token);
    	
    	headers.setContentType(MediaType.APPLICATION_JSON);
    	HttpEntity<String> entity = new HttpEntity<>("body", headers);
    	
    	ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
    	System.out.println(response);
    	if (response.toString().contains("O token informado é inválido") || response.toString().contains("expirou")) {
    		myresponse.getErrors().add(response.toString());
    		return myresponse;
    	}
    	
    	JSONObject personJsonObject = new JSONObject(response.getBody());
    	personJsonObject = (JSONObject) personJsonObject.get("output");
    	personJsonObject = (JSONObject) personJsonObject.get("result");
    	
    	JSONArray arrayjs = personJsonObject.getJSONArray("usuarios");
    	arrayjs.forEach(el -> {
    		JSONObject jobj = (JSONObject) el;
    		String login = (String) jobj.get("login");
    		usuarios.add(new User(login));    		
    	});
    	
    	myresponse.setData(usuarios);
    	return myresponse;
    }
    
    
    public String getAvatar(String login, String token) {
    	String uri = "https://www.dminerweb.com.br:8553/api/auth/avatar/?login_user=" + login;
    	RestTemplate restTemplate = new RestTemplate();
    	Response<List<User>> myresponse = new Response<>();
    	HttpHeaders headers = new HttpHeaders();
    	headers.add("BAERER_AUTHENTICATION", token);
    	
    	headers.setContentType(MediaType.APPLICATION_JSON);
    	HttpEntity<String> entity = new HttpEntity<>("body", headers);
    	
    	ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
    	return response.getBody();    	
    }
    
    
    
}
