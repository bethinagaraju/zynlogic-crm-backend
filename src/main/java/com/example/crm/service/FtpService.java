package com.example.crm.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FtpService {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String publicUrlBase;

    public FtpService(@Value("${hostinger.ftp.host}") String host,
                      @Value("${hostinger.ftp.port:21}") int port,
                      @Value("${hostinger.ftp.username}") String username,
                      @Value("${hostinger.ftp.password}") String password,
                      @Value("${hostinger.ftp.crm-public-url:}") String publicUrlBase) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.publicUrlBase = (publicUrlBase == null) ? "" : publicUrlBase;
    }

    public String upload(MultipartFile file, String remoteDir) throws IOException {
        if (remoteDir == null || remoteDir.isBlank()) remoteDir = "/";
        String original = file.getOriginalFilename();
        String safeName = System.currentTimeMillis() + "_" + (original == null ? "file" : original.replaceAll("\\s+", "_"));

        FTPClient ftp = new FTPClient();
        try {
            ftp.connect(host, port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("FTP server refused connection, reply: " + reply);
            }
            if (!ftp.login(username, password)) {
                throw new IOException("FTP login failed");
            }
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            // Ensure directories exist
            String dirToCreate = remoteDir;
            if (!dirToCreate.startsWith("/")) dirToCreate = "/" + dirToCreate;
            String[] parts = dirToCreate.split("/");
            String cur = "";
            for (String p : parts) {
                if (p == null || p.isEmpty()) continue;
                cur += "/" + p;
                if (!ftp.changeWorkingDirectory(cur)) {
                    ftp.makeDirectory(cur);
                }
            }

            ftp.changeWorkingDirectory(dirToCreate);

            try (InputStream in = file.getInputStream()) {
                boolean ok = ftp.storeFile(safeName, in);
                if (!ok) {
                    throw new IOException("FTP upload failed: " + ftp.getReplyString());
                }
            }

            String base = publicUrlBase == null ? "" : publicUrlBase.replaceAll("/+$", "");
            String dirPart = dirToCreate.replaceAll("/+$", "");
            String url = base.isEmpty() ? (dirPart + "/" + safeName) : (base + (dirPart.equals("") ? "" : dirPart) + "/" + safeName);
            // Normalize double slashes
            url = url.replaceAll("([^:])/+", "$1/");
            return url;
        } finally {
            if (ftp.isConnected()) {
                try { ftp.logout(); ftp.disconnect(); } catch (IOException ex) { }
            }
        }
    }

    public boolean deleteByPublicUrl(String publicUrl) throws IOException {
        if (publicUrl == null || publicUrl.isBlank()) return false;

        // Determine remote path from publicUrl and configured publicUrlBase
        String base = (publicUrlBase == null) ? "" : publicUrlBase.replaceAll("/+$", "");
        String path = publicUrl;
        if (!base.isBlank() && publicUrl.startsWith(base)) {
            path = publicUrl.substring(base.length());
        }

        // Ensure leading slash
        if (!path.startsWith("/")) path = "/" + path;

        FTPClient ftp = new FTPClient();
        try {
            ftp.connect(host, port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("FTP server refused connection, reply: " + reply);
            }
            if (!ftp.login(username, password)) {
                throw new IOException("FTP login failed");
            }
            ftp.enterLocalPassiveMode();

            boolean deleted = ftp.deleteFile(path);
            return deleted;
        } finally {
            if (ftp.isConnected()) {
                try { ftp.logout(); ftp.disconnect(); } catch (IOException ex) { }
            }
        }
    }

    public boolean deleteByFilename(String remoteDir, String filename) throws IOException {
        if (filename == null || filename.isBlank()) return false;

        String dir = (remoteDir == null || remoteDir.isBlank()) ? "/" : remoteDir;
        if (!dir.startsWith("/")) dir = "/" + dir;
        // Normalize dir to not end with slash
        dir = dir.replaceAll("/+$", "");
        String path = dir + "/" + filename;

        FTPClient ftp = new FTPClient();
        try {
            ftp.connect(host, port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("FTP server refused connection, reply: " + reply);
            }
            if (!ftp.login(username, password)) {
                throw new IOException("FTP login failed");
            }
            ftp.enterLocalPassiveMode();

            boolean deleted = ftp.deleteFile(path);
            return deleted;
        } finally {
            if (ftp.isConnected()) {
                try { ftp.logout(); ftp.disconnect(); } catch (IOException ex) { }
            }
        }
    }

}
