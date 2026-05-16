package com.example.enrollment.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

//서비스 로직에서 예외 던질 때 쓸 공통 예외
@Getter
public class BusinessException extends RuntimeException{

    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
