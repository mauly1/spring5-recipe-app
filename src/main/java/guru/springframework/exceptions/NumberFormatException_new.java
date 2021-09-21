package guru.springframework.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NumberFormatException_new extends RuntimeException{
    public NumberFormatException_new() {
        super();
    }

    public NumberFormatException_new(String message) {
        super(message);
    }

    public NumberFormatException_new(String message, Throwable cause) {
        super(message, cause);
    }
}
