package com.example.router.service;


import com.example.router.config.IncomingAuthProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class IncomingAuthService{

    private final IncomingAuthProperties incomingAuthProperties;

    public IncomingAuthService(IncomingAuthProperties incomingAuthProperties) {
        this.incomingAuthProperties = incomingAuthProperties;
    }

    public boolean isAuthorized(String actualSenderId,
                                String actualPassword) {

        boolean senderOk = MessageDigest.isEqual(
                incomingAuthProperties.getSenderId().getBytes(StandardCharsets.UTF_8),
                actualSenderId.getBytes(StandardCharsets.UTF_8));

        boolean passwordOk = MessageDigest.isEqual(
                incomingAuthProperties.getPassword().getBytes(StandardCharsets.UTF_8),
                actualPassword.getBytes(StandardCharsets.UTF_8));

        if (!senderOk || !passwordOk) {
            throw new IllegalArgumentException("Авторизация не выполнена: неверный senderId или пароль.");
        }

        return true;
    }

}
