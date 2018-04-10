package proj4;

import java.util.Date;

public class Response {
    public String code;
    public String status;
    public String connection;
    public Date date;
    public Date lastModified;
    public String contentType;
    public int length;
    public byte data[];

    public String toString() {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ");
        response.append(code);
        response.append(" ");
        response.append(status);
        response.append("\r\n");

        response.append("Connection: ");
        response.append(connection);
        response.append("\r\n");

        response.append("Content-Type: ");
        response.append(contentType);
        response.append("\r\n");

        response.append("Content-Length: ");
        response.append(length);
        response.append("\r\n");

        response.append("Last-Modified: ");
        response.append(lastModified);
        response.append("\r\n");

        response.append("Date: ");
        response.append(date);
        response.append("\r\n");

        response.append("\r\n");
        response.append(data);

        return response.toString();
    }
}
