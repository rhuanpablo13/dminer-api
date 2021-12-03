package com.dminer.services;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.dminer.dto.UserDTO;
import com.dminer.dto.UserReductDTO;
import com.dminer.entities.User;
import com.dminer.repository.GenericRepositoryPostgres;
import com.dminer.repository.GenericRepositorySqlServer;
import com.dminer.repository.PermissionRepository;
import com.dminer.repository.UserRepository;
import com.dminer.rest.model.users.UserRestModel;
import com.dminer.services.interfaces.IUserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Service
public class UserService implements IUserService {

    @Autowired
	private UserRepository userRepository;
    
    @Autowired
	private PermissionRepository permissionRepository;
	
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
    
    
    
    public List<UserDTO> search(String termo, String token) {    	
    	UserRestModel model = carregarUsuariosApi(token);    	
    	List<UserDTO> pesquisa = new ArrayList<UserDTO>();
    	
    	if (model == null || model.hasError()) {
        	return pesquisa;
        }
    	
    	if (termo == null) {
    		model.getOutput().getResult().getUsuarios().forEach(m -> {
    			pesquisa.add(m.toUserDTO());
    		});
    		return pesquisa;
    	}
    	
    	termo = termo.toLowerCase();
    	for (UserDTO u : pesquisa) {
    		String concat = (u.getArea() + " " + u.getBirthDate() + " " + u.getEmail() + " " +
    				u.getLinkedinUrl() + " " + u.getLogin() + " " + u.getPermission()).toLowerCase();    		
    		if (concat.contains(termo)) {
    			pesquisa.add(u);
    		}
		}    	    	
    	return pesquisa;
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
    
    
    public UserRestModel carregarUsuariosApi(String token) {
    	
    	String uri = "https://www.dminerweb.com.br:8553/api/administrative/client_area/user/select_user";		
		try {
			URL url = new URL(uri);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestProperty("BAERER_AUTHENTICATION", token);
			InputStream stream = connection.getInputStream();
			Scanner scanner = new Scanner(stream);
			
			String response = "";
			while (scanner.hasNext()) {
				response += scanner.next();
			}
			scanner.close();
			if (response.contains("expirou")) {
				return null;
			}
			
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			UserRestModel staff = gson.fromJson(response, UserRestModel.class);
			System.out.println(staff.toString());
			return staff;
			
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
        return null;
    }
    
    
    
    public List<UserReductDTO> carregarUsuariosApiReduct(String token) {
        log.info("Recuperando todos os usuário reduzidos na api externa");
        
        List<UserReductDTO> usuarios = new ArrayList<>();        
        UserRestModel model = carregarUsuariosApi(token);
        System.out.println(model.toString());
        if (model == null || model.hasError()) {
        	return null;
        }
        
        model.getOutput().getResult().getUsuarios().forEach(u -> {
        	UserReductDTO dto = new UserReductDTO();
        	dto.setLogin(u.getLogin());
        	dto.setUsername(u.getUserName());
        	usuarios.add(dto);
        });
    	return usuarios;
    }
    
    
    public byte[] getAvatar(String login) {
//    	return "123".getBytes();
    	try {
    		BufferedImage image = ImageIO.read(new URL("https://www.dminerweb.com.br:8553/api/auth/avatar/?login_user=" + login));
    		if (image != null) {
    			ByteArrayOutputStream baos = new ByteArrayOutputStream();
    			ImageIO.write(image, "png", baos);
    			return baos.toByteArray();
    		}
    	} catch (IOException e) {}
    	return null;
    }
    
    public byte[] getBanner(String login) {
    	return "123".getBytes();
    	
//		User user = userRepository.findByLogin(login);
//		if (user == null || user.getBanner() == null) 
//			return null;
//		return user.getBanner().getBytes();
    }
    
    public void compress(byte[] bytes) throws IOException {
    	
    	InputStream is = new ByteArrayInputStream(bytes);
        BufferedImage image = ImageIO.read(is);
        OutputStream os = new FileOutputStream("compressed_image.png");

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        ImageWriter writer = (ImageWriter) writers.next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.0f);  // Change the quality value you prefer
        writer.write(null, new IIOImage(image, null, null), param);

        os.close();
        ios.close();
        writer.dispose();
    }
    
    
    public static void resize(String inputImagePath,
            String outputImagePath, double percent) throws IOException {
        File inputFile = new File(inputImagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);
        int scaledWidth = (int) (inputImage.getWidth() * percent);
        int scaledHeight = (int) (inputImage.getHeight() * percent);
        resize(inputImagePath, outputImagePath, scaledWidth, scaledHeight);
    }
    
    public static void resize(String inputImagePath,
            String outputImagePath, int scaledWidth, int scaledHeight)
            throws IOException {
        // reads input image
        File inputFile = new File(inputImagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);
 
        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth,
                scaledHeight, inputImage.getType());
 
        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();
 
        // extracts extension of output file
        String formatName = outputImagePath.substring(outputImagePath
                .lastIndexOf(".") + 1);
 
        // writes to output file
        ImageIO.write(outputImage, formatName, new File(outputImagePath));
    }
}
