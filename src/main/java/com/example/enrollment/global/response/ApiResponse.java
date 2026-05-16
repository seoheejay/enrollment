package com.example.enrollment.global.response;

import lombok.Getter;

//api 응답 일관되게 감싸주는 클래스
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;

    public ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> ok(T data){
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(String message){
        return new ApiResponse<>(false, null, message);
    }

}
