package com.mipyme.company;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@SuppressWarnings("deprecation")
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PlanLimitException extends RuntimeException {

    public PlanLimitException(String message) {
        super(message);
    }
}
