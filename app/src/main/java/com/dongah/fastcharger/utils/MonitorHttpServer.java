package com.dongah.fastcharger.utils;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MonitorHttpServer extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(MonitorHttpServer.class);
    private final int port;
    private boolean running = true;
    private final Gson gson = new Gson();

    public MonitorHttpServer(int port) {
        this.port = port;
    }

    public void stopServer() {
        running = false;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("MonitorHttpServer started on port {}", port);
            logLocalIpAddress();
            while (running) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleRequest(clientSocket);
                } catch (Exception e) {
                    if (running) {
                        logger.error("Error handling request: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("MonitorHttpServer error: {}", e.getMessage());
        }
    }

    private void handleRequest(Socket client) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        String line = in.readLine();
        if (line == null) return;

        String[] parts = line.split(" ");
        if (parts.length < 2) return;
        String path = parts[1];

        if (path.equals("/api/data")) {
            sendJsonResponse(client);
        } else {
            sendHtmlResponse(client);
        }
    }

    private void sendJsonResponse(Socket client) throws Exception {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        if (activity == null) return;

        Map<String, Object> data = new HashMap<>();
        int maxCh = GlobalVariables.maxChannel;
        
        Map<String, Object>[] channels = new Map[maxCh];
        for (int i = 0; i < maxCh; i++) {
            Map<String, Object> chData = new HashMap<>();
            chData.put("rx", activity.getControlBoard().getRxData(i));
            chData.put("tx", activity.getControlBoard().getTxData(i));
            channels[i] = chData;
        }
        data.put("channels", channels);
        data.put("timestamp", System.currentTimeMillis());

        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        OutputStream out = client.getOutputStream();
        out.write("HTTP/1.1 200 OK\r\n".getBytes());
        out.write("Content-Type: application/json\r\n".getBytes());
        out.write(("Content-Length: " + bytes.length + "\r\n").getBytes());
        out.write("Access-Control-Allow-Origin: *\r\n".getBytes());
        out.write("\r\n".getBytes());
        out.write(bytes);
        out.flush();
    }

    private void sendHtmlResponse(Socket client) throws Exception {
        String html = getDashboardHtml();
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

        OutputStream out = client.getOutputStream();
        out.write("HTTP/1.1 200 OK\r\n".getBytes());
        out.write("Content-Type: text/html; charset=UTF-8\r\n".getBytes());
        out.write(("Content-Length: " + bytes.length + "\r\n").getBytes());
        out.write("\r\n".getBytes());
        out.write(bytes);
        out.flush();
    }

    private String getDashboardHtml() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>ControlBoard Monitor</title>\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "    <style>\n" +
                "        body { font-family: sans-serif; background: #121212; color: #e0e0e0; margin: 20px; }\n" +
                "        .container { display: flex; flex-wrap: wrap; gap: 20px; }\n" +
                "        .channel-card { background: #1e1e1e; border-radius: 8px; padding: 15px; flex: 1; min-width: 300px; box-shadow: 0 4px 6px rgba(0,0,0,0.3); }\n" +
                "        h2 { color: #03dac6; border-bottom: 1px solid #333; padding-bottom: 10px; }\n" +
                "        .data-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 5px; font-size: 0.9em; }\n" +
                "        .label { color: #bb86fc; }\n" +
                "        .value { text-align: right; font-family: monospace; }\n" +
                "        .header { display: flex; justify-content: space-between; align-items: center; }\n" +
                "        .status-on { color: #4caf50; font-weight: bold; }\n" +
                "        .status-off { color: #f44336; }\n" +
                "        .refresh-tag { font-size: 0.7em; color: #777; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"header\">\n" +
                "        <h1>ControlBoard Monitor</h1>\n" +
                "        <div id=\"last-update\" class=\"refresh-tag\">Waiting for data...</div>\n" +
                "    </div>\n" +
                "    <div class=\"container\" id=\"monitor-container\"></div>\n" +
                "\n" +
                "    <script>\n" +
                "        function updateMonitor() {\n" +
                "            fetch('/api/data')\n" +
                "                .then(response => response.json())\n" +
                "                .then(data => {\n" +
                "                    const container = document.getElementById('monitor-container');\n" +
                "                    container.innerHTML = '';\n" +
                "                    document.getElementById('last-update').innerText = 'Last Update: ' + new Date(data.timestamp).toLocaleTimeString();\n" +
                "\n" +
                "                    data.channels.forEach((ch, index) => {\n" +
                "                        const card = document.createElement('div');\n" +
                "                        card.className = 'channel-card';\n" +
                "                        \n" +
                "                        let html = `<h2>Channel ${index + 1}</h2>`;\n" +
                "                        \n" +
                "                        html += '<h3>Rx Data</h3><div class=\"data-grid\">';\n" +
                "                        for (const [key, value] of Object.entries(ch.rx)) {\n" +
                "                            if (typeof value === 'object') continue;\n" +
                "                            html += `<div class=\"label\">${key}</div><div class=\"value\">${formatValue(value)}</div>`;\n" +
                "                        }\n" +
                "                        html += '</div>';\n" +
                "\n" +
                "                        html += '<h3>Tx Data</h3><div class=\"data-grid\">';\n" +
                "                        for (const [key, value] of Object.entries(ch.tx)) {\n" +
                "                            if (typeof value === 'object') continue;\n" +
                "                            html += `<div class=\"label\">${key}</div><div class=\"value\">${formatValue(value)}</div>`;\n" +
                "                        }\n" +
                "                        html += '</div>';\n" +
                "                        \n" +
                "                        card.innerHTML = html;\n" +
                "                        container.appendChild(card);\n" +
                "                    });\n" +
                "                })\n" +
                "                .catch(err => console.error('Fetch error:', err));\n" +
                "        }\n" +
                "\n" +
                "        function formatValue(val) {\n" +
                "            if (typeof val === 'boolean') {\n" +
                "                return val ? '<span class=\"status-on\">ON</span>' : '<span class=\"status-off\">OFF</span>';\n" +
                "            }\n" +
                "            return val;\n" +
                "        }\n" +
                "\n" +
                "        setInterval(updateMonitor, 1000);\n" +
                "        updateMonitor();\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    // MonitorHttpServer.java의 run() 메소드 시작 부분에 추가하면 좋습니다.
    private void logLocalIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                java.net.NetworkInterface intf = en.nextElement();
                java.util.Enumeration<java.net.InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    java.net.InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        logger.info("Monitor Dashboard available at: http://{}:{}", inetAddress.getHostAddress(), port);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Could not get local IP address: {}", ex.getMessage());
        }
    }

}
