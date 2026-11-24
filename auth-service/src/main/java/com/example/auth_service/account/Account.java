package com.example.auth_service.account;
import jakarta.persistence.*;
@Entity @Table(name="accounts")
public class Account {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=60) private String username;
    @Column(name="password_hash", nullable=false, length=100) private String passwordHash;
    @Column(nullable=false, length=20) private String role;
    @Column(nullable=false) private boolean enabled = true;
    @Column(name="display_name",length=100) private String displayName;
    @Column(length=20) private String phone;
    @Column(length=254) private String email;
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getDisplayName(){return displayName;}
    public void setDisplayName(String value){displayName=value;}
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPhone(){return phone;}
    public void setPhone(String value){phone=value;}
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getEmail(){return email;}
    public void setEmail(String value){email=value;}
    public void setPasswordHash(String value){passwordHash=value;}
    public Account() {}
    public Account(String username, String hash, String role) {
        this.username=username; this.passwordHash=hash; this.role=role;
    }
    public Long getId(){return id;}
    public String getUsername(){return username;}
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPasswordHash(){return passwordHash;}
    public String getRole(){return role;}
    public boolean isEnabled(){return enabled;}
    public void setEnabled(boolean enabled){this.enabled=enabled;}
}
