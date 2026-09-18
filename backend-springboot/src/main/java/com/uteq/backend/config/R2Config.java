package com.uteq.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Configuración del cliente S3 compatible con Cloudflare R2 para los respaldos.
 * Lee el endpoint y las credenciales desde las propiedades {@code app.backup.r2}.
 */
@Configuration
public class R2Config {
    @Value("${app.backup.r2.endpoint:}") private String endpoint;
    @Value("${app.backup.r2.access-key:}") private String accessKey;
    @Value("${app.backup.r2.secret-key:}") private String secretKey;
    /**
     * Crea el cliente S3 hacia R2 con el endpoint configurado, credenciales estáticas,
     * región automática y acceso estilo path. Devuelve null si falta el endpoint o la
     * clave de acceso, dejando los respaldos remotos deshabilitados.
     *
     * @return cliente S3 configurado, o null si R2 no está configurado
     */
    @Bean
    public S3Client s3Client() {
        if (endpoint == null || endpoint.isBlank() || accessKey == null || accessKey.isBlank()) return null;
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}
