package com.osigie.rehook.exception;


public class UserAlreadyExistException extends RuntimeException {
    public final String message = "User Already Exists!";

    @Override
    public String getMessage() {
        return this.message;
    }
}
