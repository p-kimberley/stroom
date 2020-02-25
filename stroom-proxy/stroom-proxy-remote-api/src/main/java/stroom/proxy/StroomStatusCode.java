package stroom.proxy;

import javax.servlet.http.HttpServletResponse;

/**
 * List of all the stroom codes that we return.
 */
public enum StroomStatusCode {
    OK(HttpServletResponse.SC_OK, 0, "OK", "Post of data successful"),

    FEED_MUST_BE_SPECIFIED(HttpServletResponse.SC_NOT_ACCEPTABLE, 100, "Feed must be specified",
            "You must provide Feed as a header argument in the request"),

    FEED_IS_NOT_DEFINED(HttpServletResponse.SC_NOT_ACCEPTABLE, 101, "Feed is not defined",
            "The feed you have provided is not known within Stroom"),

    FEED_IS_NOT_SET_TO_RECEIVED_DATA(HttpServletResponse.SC_NOT_ACCEPTABLE, 110, "Feed is not set to receive data",
            "The feed you have provided has not been setup to receive data"),

    UNKNOWN_COMPRESSION(HttpServletResponse.SC_NOT_ACCEPTABLE, 200, "Unknown compression",
            "Compression argument must be one of ZIP, GZIP and NONE"),

    CLIENT_CERTIFICATE_REQUIRED(HttpServletResponse.SC_UNAUTHORIZED, 300, "Client Certificate Required",
            "The feed you have provided requires a client HTTPS certificate to send data"),

    CLIENT_CERTIFICATE_NOT_AUTHORISED(HttpServletResponse.SC_FORBIDDEN, 310, "Client Certificate not authorised",
            "The feed you have provided does not allow your client certificate to send data"),

    COMPRESSED_STREAM_INVALID(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 400, "Compressed stream invalid",
            "The stream of data sent does not form a valid compressed file.  Maybe it terminated unexpectedly or is corrupt."),

    UNKNOWN_ERROR(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 999, "Unknown error",
            "An unknown unexpected error occurred");

    public static StroomStatusCode getStroomStatusCode(int code) {
        for (StroomStatusCode stroomStatusCode : StroomStatusCode.values()) {
            if (stroomStatusCode.getCode() == code) {
                return stroomStatusCode;
            }
        }
        return UNKNOWN_ERROR;
    }

    private final String message;
    private final String reason;
    private final int code;
    private final int httpCode;

    private StroomStatusCode(final int httpCode, final int code, final String message, final String reason) {
        this.httpCode = httpCode;
        this.code = code;
        this.message = message;
        this.reason = reason;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return httpCode + " - " + code + " - " + message;
    }

    public static void main(String[] args) {
        System.out.println("{| class=\"wikitable\"");
        System.out.println("!HTTP Status!!Stroom-Status!!Message!!Reason");
        for (StroomStatusCode stroomStatusCode : StroomStatusCode.values()) {
            System.out.println("|-");
            System.out.println("|" + stroomStatusCode.getHttpCode() + "||" + stroomStatusCode.getCode() + "||"
                    + stroomStatusCode.getMessage() + "||" + stroomStatusCode.getReason());

        }
        System.out.println("|}");
    }
}
