package com.toghrakx.backend.service;

import com.toghrakx.backend.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Uploads go to the temporary folder set in application-test.properties.
@IntegrationTest
class ImageUploadValidationTest {

    @Autowired
    private ImageService imageService;

    @Test
    void realPngIsAccepted() throws IOException {
        String url = imageService.storeFile(new MockMultipartFile("file", "photo.png", "image/png", png()));
        assertThat(url).endsWith(".png");
    }

    @Test
    void storedExtensionComesFromTheBytesNotTheFileName() throws IOException {
        // A real PNG uploaded with a .jpg name is stored as .png
        String url = imageService.storeFile(new MockMultipartFile("file", "photo.jpg", "image/jpeg", png()));
        assertThat(url).endsWith(".png");
    }

    @Test
    void renamedNonImageIsRejected() {
        byte[] script = "<script>alert('x')</script>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> imageService.storeFile(
                new MockMultipartFile("file", "evil.png", "image/png", script)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a valid");
    }

    @Test
    void webpSignatureIsAccepted() {
        byte[] webp = { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' ' };
        String url = imageService.storeFile(new MockMultipartFile("file", "photo.webp", "image/webp", webp));
        assertThat(url).endsWith(".webp");
    }

    private static byte[] png() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
