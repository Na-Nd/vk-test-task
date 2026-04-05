package ru.nand.vktesttask.exception;

public class TarantoolOperationException extends RuntimeException{

    public TarantoolOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
