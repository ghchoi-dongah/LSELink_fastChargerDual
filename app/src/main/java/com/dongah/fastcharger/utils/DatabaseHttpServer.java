package com.dongah.fastcharger.utils;

import android.database.Cursor;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.sqlite.SQLiteHelper;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHttpServer extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHttpServer.class);

    private static final int PORT = 8081;
    private static final String[] TABLES = {
            "CP_UNIT_PRICE", "CP_CHANGE_MODE", "CP_CHG_ELECMODE", "CP_RECHG_SOC"
    };

    private boolean running = true;
    private final Gson gson = new Gson();

    public void stopServer() {
        running = false;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("DatabaseHttpServer started on port {}", PORT);
            logLocalIp();
            while (running) {
                try (Socket client = serverSocket.accept()) {
                    handleRequest(client);
                } catch (Exception e) {
                    if (running) logger.error("DatabaseHttpServer request error: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("DatabaseHttpServer error: {}", e.getMessage());
        }
    }

    private void handleRequest(Socket client) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        String line = in.readLine();
        if (line == null) return;

        String[] parts = line.split(" ");
        if (parts.length < 2) return;

        String[] pathParts = parts[1].split("\\?");
        String path  = pathParts[0];
        String query = pathParts.length > 1 ? pathParts[1] : null;

        if (path.equals("/api/db")) {
            sendDbJson(client, query);
        } else {
            sendHtml(client);
        }
    }

    // ── DB API ─────────────────────────────────────────────────────────────────

    private void sendDbJson(Socket client, String query) throws Exception {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        if (activity == null) return;

        SQLiteHelper helper = SQLiteHelper.getInstance(activity);
        Map<String, Object> result = new HashMap<>();

        String tableName = null;
        if (query != null && query.startsWith("table=")) {
            tableName = query.substring(6);
        }

        if (tableName != null) {
            result.put("table", tableName);
            result.put("data", queryTable(helper, tableName));
        } else {
            Map<String, Object> all = new HashMap<>();
            for (String t : TABLES) all.put(t, queryTable(helper, t));
            result.put("tables", all);
        }
        result.put("timestamp", System.currentTimeMillis());

        String json = gson.toJson(result);
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

    private Map<String, Object> queryTable(SQLiteHelper helper, String tableName) {
        Map<String, Object> result = new HashMap<>();
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = helper.selectAll(tableName);
            if (cursor != null) {
                String[] colNames = cursor.getColumnNames();
                for (String col : colNames) columns.add(col);
                while (cursor.moveToNext()) {
                    Map<String, Object> row = new HashMap<>();
                    for (String col : colNames) {
                        int idx = cursor.getColumnIndex(col);
                        switch (cursor.getType(idx)) {
                            case Cursor.FIELD_TYPE_INTEGER: row.put(col, cursor.getLong(idx));   break;
                            case Cursor.FIELD_TYPE_FLOAT:   row.put(col, cursor.getDouble(idx)); break;
                            case Cursor.FIELD_TYPE_NULL:    row.put(col, null);                  break;
                            default:                        row.put(col, cursor.getString(idx)); break;
                        }
                    }
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        result.put("columns", columns);
        result.put("rows", rows);
        return result;
    }

    // ── HTML ───────────────────────────────────────────────────────────────────

    private void sendHtml(Socket client) throws Exception {
        String html = buildHtml();
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        OutputStream out = client.getOutputStream();
        out.write("HTTP/1.1 200 OK\r\n".getBytes());
        out.write("Content-Type: text/html; charset=UTF-8\r\n".getBytes());
        out.write(("Content-Length: " + bytes.length + "\r\n").getBytes());
        out.write("\r\n".getBytes());
        out.write(bytes);
        out.flush();
    }

    private String buildHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
                .append("<html><head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n")
                .append("<title>Database Inspector</title>\n")
                .append("<style>\n")
                .append("  *{box-sizing:border-box;}\n")
                .append("  body{font-family:sans-serif;background:#121212;color:#e0e0e0;margin:0;padding:16px;}\n")
                .append("  h1{margin:0 0 16px;font-size:1.3em;color:#03dac6;}\n")
                // table selector
                .append("  .bar{display:flex;align-items:center;gap:10px;margin-bottom:16px;flex-wrap:wrap;}\n")
                .append("  .bar select{background:#1e1e1e;color:#e0e0e0;border:1px solid #444;padding:8px 12px;border-radius:6px;font-size:.95em;cursor:pointer;}\n")
                .append("  .bar button{background:#03dac6;color:#000;border:none;padding:8px 18px;border-radius:6px;cursor:pointer;font-weight:bold;font-size:.95em;}\n")
                .append("  .bar button:hover{background:#04f0d8;}\n")
                .append("  .badge{background:#2a2a2a;border-radius:12px;padding:3px 10px;font-size:.78em;color:#aaa;}\n")
                .append("  .ts{font-size:.75em;color:#666;margin-left:auto;}\n")
                // table
                .append("  .wrap{overflow-x:auto;border-radius:8px;border:1px solid #2a2a2a;}\n")
                .append("  table{border-collapse:collapse;width:100%;font-size:.85em;}\n")
                .append("  thead tr{background:#1e1e1e;}\n")
                .append("  th{color:#bb86fc;padding:10px 14px;text-align:left;white-space:nowrap;border-bottom:1px solid #333;font-weight:600;}\n")
                .append("  td{padding:8px 14px;border-bottom:1px solid #1e1e1e;font-family:monospace;}\n")
                .append("  tbody tr:hover td{background:#1a1a2e;}\n")
                .append("  .null{color:#444;font-style:italic;}\n")
                .append("  .empty{text-align:center;padding:32px;color:#555;font-size:.95em;}\n")
                .append("  .err{color:#cf6679;padding:12px;background:#2a1e1e;border-radius:6px;font-size:.85em;}\n")
                .append("</style>\n")
                .append("</head><body>\n")
                .append("<h1>Database Inspector</h1>\n")
                .append("<div class=\"bar\">\n")
                .append("  <select id=\"tbl\">\n");

        for (String t : TABLES) {
            sb.append("    <option>").append(t).append("</option>\n");
        }

        sb.append("  </select>\n")
                .append("  <button onclick=\"load()\">조회</button>\n")
                .append("  <span id=\"badge\" class=\"badge\" style=\"display:none\"></span>\n")
                .append("  <span id=\"ts\" class=\"ts\"></span>\n")
                .append("</div>\n")
                .append("<div id=\"content\"><div class=\"empty\">테이블을 선택하고 조회 버튼을 누르세요</div></div>\n")
                .append("<script>\n")
                .append("function load(){\n")
                .append("  const t=document.getElementById('tbl').value;\n")
                .append("  document.getElementById('content').innerHTML='<div class=\"empty\">Loading...</div>';\n")
                .append("  fetch('/api/db?table='+t)\n")
                .append("    .then(r=>r.json())\n")
                .append("    .then(res=>render(res))\n")
                .append("    .catch(e=>{\n")
                .append("      document.getElementById('content').innerHTML='<div class=\"err\">오류: '+e.message+'</div>';\n")
                .append("    });\n")
                .append("}\n")
                .append("function render(res){\n")
                .append("  const d=res.data;\n")
                .append("  document.getElementById('ts').innerText='조회: '+new Date(res.timestamp).toLocaleTimeString();\n")
                .append("  const badge=document.getElementById('badge');\n")
                .append("  const wrap=document.getElementById('content');\n")
                .append("  if(d.error){\n")
                .append("    wrap.innerHTML='<div class=\"err\">DB 오류: '+d.error+'</div>';\n")
                .append("    badge.style.display='none'; return;\n")
                .append("  }\n")
                .append("  if(!d.columns||d.columns.length===0){\n")
                .append("    wrap.innerHTML='<div class=\"empty\">컬럼 정보 없음</div>';\n")
                .append("    badge.style.display='none'; return;\n")
                .append("  }\n")
                .append("  const cnt=d.rows?d.rows.length:0;\n")
                .append("  badge.innerText=cnt+' rows'; badge.style.display='';\n")
                .append("  let h='<div class=\"wrap\"><table><thead><tr>';\n")
                .append("  d.columns.forEach(c=>h+='<th>'+c+'</th>');\n")
                .append("  h+='</tr></thead><tbody>';\n")
                .append("  if(cnt>0){\n")
                .append("    d.rows.forEach(row=>{\n")
                .append("      h+='<tr>'+d.columns.map(c=>{\n")
                .append("        const v=row[c];\n")
                .append("        return '<td>'+(v!=null?v:'<span class=\"null\">null</span>')+'</td>';\n")
                .append("      }).join('')+'</tr>';\n")
                .append("    });\n")
                .append("  } else {\n")
                .append("    h+='<tr><td colspan=\"'+d.columns.length+'\" class=\"empty\">데이터 없음</td></tr>';\n")
                .append("  }\n")
                .append("  h+='</tbody></table></div>';\n")
                .append("  wrap.innerHTML=h;\n")
                .append("}\n")
                // 페이지 로드 시 첫 번째 테이블 자동 조회
                .append("window.onload=()=>load();\n")
                .append("</script>\n")
                .append("</body></html>\n");
        return sb.toString();
    }

    private void logLocalIp() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                java.net.NetworkInterface intf = en.nextElement();
                java.util.Enumeration<java.net.InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                        logger.info("DatabaseHttpServer available at: http://{}:{}", addr.getHostAddress(), PORT);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("logLocalIp error: {}", e.getMessage());
        }
    }
}
