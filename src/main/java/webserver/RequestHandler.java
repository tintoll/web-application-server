package webserver;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            DataOutputStream dos = new DataOutputStream(out);

            // 요구사항1 - index.html 파일을 읽어 클라이언트에 응답한다.
            // http://localhost:8080/index.html  <- 주소를 캐치해야한다.
            // 요청헤더 내용
            /*
             * GET /index.html HTTP/1.1
             * Host: localhost:8080
             * Connection: keep-alive
             * ...
             */
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            byte[] body = null;

            String line = br.readLine();

    		    String[] array = line.split(" ");
            String action = array[0];
            String path = array[1];
            if(action.equals("GET") && !path.equals("/")) {
            		// 파일을 읽는다.
				File file = new File("./webapp"+array[1]);
				body =Files.readAllBytes(file.toPath());
            }

            int contentLength = 0;
            while( !"".equals(line = br.readLine())){
	            	if(action.equals("POST") && path.equals("/user/create")) {
            			String[] arr = line.split(":");
            			if(arr[0].equals("Content-Length")) {
            				contentLength = Integer.parseInt(arr[1].substring(1, arr[1].length()));
            			}
	            }

            		if(line == null) return;
            }

            if(action.equals("POST") && path.equals("/user/create")) {
            		String queryString = IOUtils.readData(br, contentLength);
            		Map<String, String> map = HttpRequestUtils.parseQueryString(queryString);
				User user = new User(map.get("userId"),
						map.get("password"),
						map.get("name"),
						map.get("email"));
				log.debug("User : {}", user);
            }


            if(body == null) body = "Hello World !!!!".getBytes();
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
