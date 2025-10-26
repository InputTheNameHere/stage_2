package org.ulpgc.bd.ingestion.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class HttpDownloader {
    private final String userAgent;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public HttpDownloader(String userAgent, int connectTimeoutMs, int readTimeoutMs) {
        this.userAgent = userAgent;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public URL findGutenbergTextURL(int bookId) {
        String[][] patterns = new String[][]{
                {"https://www.gutenberg.org/files/%d/%d-0.txt", "%d", "%d"},
                {"https://www.gutenberg.org/files/%d/%d.txt", "%d", "%d"},
                {"https://www.gutenberg.org/files/%d/%d-8.txt", "%d", "%d"},
                {"https://www.gutenberg.org/ebooks/%d.txt", "%d", null}
        };
        for (String[] pat : patterns) {
            String fmt = pat[0];
            String s = pat[2] == null ? String.format(fmt, bookId) : String.format(fmt, bookId, bookId);
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    URL url = new URL(s);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestMethod("HEAD");
                    conn.setConnectTimeout(connectTimeoutMs);
                    conn.setReadTimeout(readTimeoutMs);
                    conn.setRequestProperty("User-Agent", userAgent);
                    int code = conn.getResponseCode();
                    if (code == HttpURLConnection.HTTP_OK) return conn.getURL();
                    if (code >= 300 && code < 400) {
                        String loc = conn.getHeaderField("Location");
                        if (loc != null) {
                            URL rurl = new URL(loc);
                            HttpURLConnection rconn = (HttpURLConnection) rurl.openConnection();
                            rconn.setInstanceFollowRedirects(true);
                            rconn.setRequestMethod("HEAD");
                            rconn.setConnectTimeout(connectTimeoutMs);
                            rconn.setReadTimeout(readTimeoutMs);
                            rconn.setRequestProperty("User-Agent", userAgent);
                            if (rconn.getResponseCode() == HttpURLConnection.HTTP_OK) return rconn.getURL();
                        }
                    }
                    if (code == HttpURLConnection.HTTP_BAD_METHOD || code == 405) {
                        HttpURLConnection gconn = (HttpURLConnection) url.openConnection();
                        gconn.setRequestMethod("GET");
                        gconn.setRequestProperty("Range", "bytes=0-0");
                        gconn.setConnectTimeout(connectTimeoutMs);
                        gconn.setReadTimeout(readTimeoutMs);
                        gconn.setRequestProperty("User-Agent", userAgent);
                        int gcode = gconn.getResponseCode();
                        if (gcode == HttpURLConnection.HTTP_PARTIAL || gcode == HttpURLConnection.HTTP_OK) return gconn.getURL();
                    }
                } catch (IOException ignored) {}
                try { Thread.sleep(300L * (1L << attempt)); } catch (InterruptedException ignored) {}
            }
        }
        return null;
    }

    public String fetchText(URL url) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(connectTimeoutMs);
                conn.setReadTimeout(readTimeoutMs);
                conn.setRequestProperty("User-Agent", userAgent);
                conn.setRequestProperty("Accept-Encoding", "gzip");
                conn.connect();
                try (InputStream base = conn.getInputStream();
                     InputStream raw = isGzip(conn) ? new GZIPInputStream(base) : base) {
                    byte[] bytes = raw.readAllBytes();
                    String ct = Optional.ofNullable(conn.getContentType()).orElse("");
                    String charset = "UTF-8";
                    Matcher m = Pattern.compile("(?i)charset=([\\w\\-]+)").matcher(ct);
                    if (m.find()) charset = m.group(1).trim();
                    try {
                        return new String(bytes, Charset.forName(charset));
                    } catch (Exception e) {
                        try { return new String(bytes, StandardCharsets.UTF_8); }
                        catch (Exception e2) { return new String(bytes, Charset.forName("ISO-8859-1")); }
                    }
                }
            } catch (IOException ioe) {
                last = ioe;
                try { Thread.sleep(300L * (1L << attempt)); } catch (InterruptedException ignored) {}
            }
        }
        throw last == null ? new IOException("Failed to fetch") : last;
    }

    private boolean isGzip(HttpURLConnection conn) {
        String enc = conn.getContentEncoding();
        return enc != null && enc.toLowerCase(Locale.ROOT).contains("gzip");
    }
}
