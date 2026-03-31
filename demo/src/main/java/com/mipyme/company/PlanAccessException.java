package com.mipyme.company;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.FORBIDDEN)
public class PlanAccessException extends RuntimeException {

    public PlanAccessException(String message) {
        super(message);
    }
}
