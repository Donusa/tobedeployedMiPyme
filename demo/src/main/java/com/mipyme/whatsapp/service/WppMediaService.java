package com.mipyme.whatsapp.service;

import com.mipyme.whatsapp.model.WppConnection;
import com.mipyme.whatsapp.model.WppMessage;
import com.mipyme.whatsapp.repository.WppConnectionRepository;
import com.mipyme.whatsapp.repository.WppMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class WppMediaService {

    private static final Logger logger = LoggerFactory.getLogger(WppMediaService.class);
    private static final String MEDIA_DIR = "wpp-media";

    private final WppCloudApiClient cloudApiClient;
    private final WppTokenService tokenService;
    private final WppConnectionRepository connectionRepository;
    private final WppMessageRepository messageRepository;

    public WppMediaService(WppCloudApiClient cloudApiClient,
            WppTokenService tokenService,
            WppConnectionRepository connectionRepository,
            WppMessageRepository messageRepository) {
        this.cloudApiClient = cloudApiClient;
        this.tokenService = tokenService;
        this.connectionRepository = connectionRepository;
        this.messageRepository = messageRepository;
    }


    @Async
    public void downloadAndStoreMedia(WppMessage message) {
        if (message.getMediaId() == null || message.getMediaId().isEmpty())
            return;

        try {

            WppConnection connection = connectionRepository.findAll().stream()
                    .filter(c -> c.getStatus() == WppConnection.WppConnectionStatus.CONNECTED)
                    .findFirst()
                    .orElse(null);

            if (connection == null) {
                logger.warn("No active WPP connection for media download");
                return;
            }

            String accessToken = tokenService.getDecryptedToken(connection);
            if (accessToken == null) {
                logger.warn("Cannot decrypt token for media download");
                return;
            }


            String mediaUrl = cloudApiClient.getMediaUrl(message.getMediaId(), accessToken);
            if (mediaUrl == null) {
                logger.warn("Could not resolve media URL for mediaId={}", message.getMediaId());
                return;
            }


            byte[] mediaBytes = cloudApiClient.downloadMedia(mediaUrl, accessToken);
            if (mediaBytes == null || mediaBytes.length == 0) {
                logger.warn("Empty media download for mediaId={}", message.getMediaId());
                return;
            }


            String ext = guessExtension(message.getMediaMimeType());
            String filename = UUID.randomUUID().toString() + ext;


            Path mediaDir = Paths.get(MEDIA_DIR);
            Files.createDirectories(mediaDir);
            Path filePath = mediaDir.resolve(filename);
            Files.write(filePath, mediaBytes);


            message.setMediaUrlLocal(filePath.toString());
            messageRepository.save(message);

            logger.info("Media downloaded and stored: {} ({} bytes)", filePath, mediaBytes.length);

        } catch (IOException e) {
            logger.error("IO error downloading media for message id={}", message.getId(), e);
        } catch (Exception e) {
            logger.error("Error downloading media for message id={}", message.getId(), e);
        }
    }

    private String guessExtension(String mimeType) {
        if (mimeType == null)
            return "";
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "audio/ogg; codecs=opus", "audio/ogg" -> ".ogg";
            case "audio/mpeg" -> ".mp3";
            case "video/mp4" -> ".mp4";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }
}
