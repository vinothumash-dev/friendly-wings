package com.friendlywings.automation.pdf.velocity;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.StringWriter;
import java.util.Map;

@Service
public class VelocityPdfService {

    private static final Logger log = LoggerFactory.getLogger(VelocityPdfService.class);

    private VelocityEngine velocityEngine;

    @PostConstruct
    public void init() {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        velocityEngine.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());
        velocityEngine.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        velocityEngine.setProperty(RuntimeConstants.ENCODING_DEFAULT, "UTF-8");
        velocityEngine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
        velocityEngine.init();
        log.info("Velocity engine initialized");
    }

    public String mergeTemplate(String templatePath, Map<String, Object> contextMap) {
        Template template = velocityEngine.getTemplate(templatePath, "UTF-8");
        VelocityContext context = new VelocityContext();
        if (contextMap != null) {
            contextMap.forEach(context::put);
        }
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }
}
