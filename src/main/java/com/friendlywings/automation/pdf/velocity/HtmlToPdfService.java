package com.friendlywings.automation.pdf.velocity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class HtmlToPdfService {

    private static final Logger log = LoggerFactory.getLogger(HtmlToPdfService.class);

    public byte[] convertToPdf(String html) throws IOException {
        // Resolve classpath image references to temp files
        html = resolveClasspathImages(html);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(os);
        renderer.finishPDF();

        return os.toByteArray();
    }

    private String resolveClasspathImages(String html) throws IOException {
        Path tempDir = Files.createTempDirectory("fw-pdf-assets");
        tempDir.toFile().deleteOnExit();

        // Find and replace classpath image references
        String result = html;
        int idx = 0;
        while ((idx = result.indexOf("classpath:", idx)) != -1) {
            int end = result.indexOf("\"", idx);
            if (end == -1) end = result.indexOf("'", idx);
            if (end == -1) break;

            String classpathUri = result.substring(idx, end);
            String path = classpathUri.replace("classpath:/", "").replace("classpath:", "");

            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is != null) {
                    String fileName = path.substring(path.lastIndexOf('/') + 1);
                    Path tempFile = tempDir.resolve(fileName);
                    Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    tempFile.toFile().deleteOnExit();
                    result = result.substring(0, idx) + tempFile.toUri().toString() + result.substring(end);
                } else {
                    log.warn("Classpath image not found: {}", path);
                    idx = end;
                }
            }
        }
        return result;
    }
}
