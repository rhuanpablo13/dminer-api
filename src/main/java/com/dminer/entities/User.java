package com.dminer.entities;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.dminer.enums.Profiles;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "USERS")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class User {
    
    @Id
	@GeneratedValue(strategy = GenerationType.AUTO)	
	private Integer id;

	@Column
	private String name; 
    
	@Column
	private Timestamp dtBirthday;

	@Column
	private byte[] avatar; 

	@Column
	private byte[] banner; 

	@Column
	private String area;

	@Column
	private String linkedin;

	@Column
	private String email;

	@Column
	private Profiles profile;


}
