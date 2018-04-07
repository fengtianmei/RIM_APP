package com.skyline.terraexplorer;

public class TEAppException extends RuntimeException {
    private static final long serialVersionUID = -3405864458883181507L;

    public TEAppException() {
        super();
    }

    public TEAppException(String message) {
        super(message);
    }

    public TEAppException(String message, Throwable cause) {
        super(message, cause);
    }

    public TEAppException(Throwable cause) {
        super(cause);
    }
}
