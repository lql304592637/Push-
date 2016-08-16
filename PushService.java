package com.sophlean.rmes.service.push;

import com.google.gson.Gson;
import com.sophlean.core.util.log.Log;
import org.springframework.context.annotation.Scope;
import org.springframework.util.StringUtils;

import javax.inject.Named;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by lql on 2016/5/18.
 */
@Named("pushService")
@Scope("singleton")
public class PushService {
    private String pushConfigFile = "D:\\PushConfig.properties";
    private Properties pushConfig = null;
    private Gson gson = new Gson();
    protected final Log log = Log.getLog(this.getClass());
    private Map<String, Map<ServerSocket, Socket>> serverMap = new HashMap<>();

    public PushService() {
        pushConfig = new Properties();
        try {
            FileInputStream fileLoad = new FileInputStream(pushConfigFile);
            pushConfig.load(fileLoad);
            fileLoad.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public int savePushConfigure(String clientId) {
        int port = 0;
        try {
            Map<ServerSocket, Socket> temp = serverMap.get(clientId);
            if (null != temp) {
                for (Map.Entry<ServerSocket, Socket> entry : temp.entrySet()) {
                    ServerSocket serverSocket = entry.getKey();
                    Socket socket = entry.getValue();
                    socket.close();
                    serverSocket.close();
                }
            }
            ServerSocket serverSocket = new ServerSocket(0);
            port = serverSocket.getLocalPort();
            FileOutputStream fileUpdate = new FileOutputStream(pushConfigFile);
            pushConfig.setProperty(clientId, port + "");
            pushConfig.store(fileUpdate, "Upadte " + clientId + "=" + port);
            fileUpdate.close();
            pushConfig.load(new FileInputStream(pushConfigFile));
            log.debug("Update push ps = " + clientId + "/" + port);
            new Thread() {
                public void run() {
                    try {
                        Socket socket = serverSocket.accept();
                        Map<ServerSocket, Socket> socketMap = new HashMap<>();
                        socketMap.put(serverSocket, socket);
                        serverMap.put(clientId, socketMap);
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            }.start();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        if (!StringUtils.isEmpty(pushConfig.get(clientId))) {
            return port;
        } else
            return 0;
    }

    private int i = 0;
    public void pushMessageToClient(String clientId, Object message) {
        if (StringUtils.isEmpty(message) || StringUtils.isEmpty(clientId)) {
            log.debug("message or clientId is null.Please check the parameter.");
            return;
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("result", "successful");
        result.put("pushInfo", message);
        String resultInfo = gson.toJson(result);

        Map<ServerSocket, Socket> socketMap = serverMap.get(clientId);
        if (null != socketMap) {
            for (Map.Entry<ServerSocket, Socket> entry : socketMap.entrySet()) {
//                PushSocketPool.doPush(new pushTask(resultInfo, entry.getValue()));
                try {
                    PrintWriter out = new PrintWriter(entry.getValue().getOutputStream(), true);
                    out.println(resultInfo);

//                    out.println(message);
                }
                catch (IOException e) {
                    log.error(e.getMessage());
                }
                System.out.println("A" + ++i);
            }
        } else {
            String port = pushConfig.getProperty(clientId);
            if (StringUtils.isEmpty(port)) {
                log.debug("Please add ip and port to PushConfig.properties for pushing message.");
                return;
            }
            int Port = Integer.parseInt(port);
            try {
                ServerSocket serverSocket = new ServerSocket(Port);
                Socket socket = serverSocket.accept();
                Map<ServerSocket, Socket> temp = new HashMap<>();
                temp.put(serverSocket, socket);
                serverMap.put(clientId, temp);
//                PushSocketPool.doPush(new pushTask(resultInfo, socket));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(resultInfo);

//                out.println(message);
                System.out.println("B" + ++i);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    class pushTask implements Runnable {
        private String message;
        private Socket socket;

        public pushTask(String message, Socket socket) {
            this.message = message;
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(message);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }
}
