package com.uteq.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackupStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadYDownload_withoutR2_guardaFileLocalSanitizado() throws Exception {
        BackupStorageService service = new BackupStorageService(null);
        ReflectionTestUtils.setField(service, "storageUrl", tempDir.toString());

        service.upload("backups/manual.zip", "contenido".getBytes());

        Path esperado = tempDir.resolve("backups_manual.zip");
        assertThat(Files.exists(esperado)).isTrue();
        assertThat(service.download("backups/manual.zip")).isEqualTo("contenido".getBytes());
    }

    @Test
    void uploadYDownload_withKeyTextPlain_cifraYDescifraLocalmente() throws Exception {
        BackupStorageService service = new BackupStorageService(null);
        ReflectionTestUtils.setField(service, "storageUrl", tempDir.toString());
        ReflectionTestUtils.setField(service, "encryptionKey", "clave-de-prueba-para-tests");

        byte[] original = "respaldo sensible".getBytes();
        service.upload("backups/cifrado.zip", original);

        byte[] almacenado = Files.readAllBytes(tempDir.resolve("backups_cifrado.zip"));
        assertThat(almacenado).isNotEqualTo(original);
        assertThat(service.download("backups/cifrado.zip")).isEqualTo(original);
    }

    @Test
    void uploadYDownload_withKeyBase64Valida_cifraYDescifraLocalmente() {
        BackupStorageService service = new BackupStorageService(null);
        ReflectionTestUtils.setField(service, "storageUrl", tempDir.toString());
        ReflectionTestUtils.setField(service, "encryptionKey",
                Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes()));

        service.upload("backup.zip", "abc".getBytes());

        assertThat(service.download("backup.zip")).isEqualTo("abc".getBytes());
    }

    @Test
    void delete_withoutR2_eliminaFileLocalSiExiste() throws Exception {
        BackupStorageService service = new BackupStorageService(null);
        ReflectionTestUtils.setField(service, "storageUrl", tempDir.toString());
        Path file = tempDir.resolve("backup.zip");
        Files.write(file, "abc".getBytes());

        service.delete("backup.zip");

        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    void download_cuandoFileNotExiste_envuelveIOException() {
        BackupStorageService service = new BackupStorageService(null);
        ReflectionTestUtils.setField(service, "storageUrl", tempDir.toString());

        assertThatThrownBy(() -> service.download("faltante.zip"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se pudo leer respaldo local");
    }

    @Test
    void isR2Configured_requiereClienteYBucketNoVacio() {
        S3Client s3Client = mock(S3Client.class);
        BackupStorageService withoutClient = new BackupStorageService(null);
        BackupStorageService withoutBucket = new BackupStorageService(s3Client);
        BackupStorageService withBlankBucket = new BackupStorageService(s3Client);
        BackupStorageService configured = new BackupStorageService(s3Client);

        ReflectionTestUtils.setField(withBlankBucket, "bucket", "   ");
        ReflectionTestUtils.setField(configured, "bucket", "sgb-backups");

        assertThat(withoutClient.isR2Configured()).isFalse();
        assertThat(withoutBucket.isR2Configured()).isFalse();
        assertThat(withBlankBucket.isR2Configured()).isFalse();
        assertThat(configured.isR2Configured()).isTrue();
    }

    @Test
    void isEncryptionEnabled_requiereClaveNoVacia() {
        BackupStorageService service = new BackupStorageService(null);

        assertThat(service.isEncryptionEnabled()).isFalse();

        ReflectionTestUtils.setField(service, "encryptionKey", "   ");
        assertThat(service.isEncryptionEnabled()).isFalse();

        ReflectionTestUtils.setField(service, "encryptionKey", "clave");
        assertThat(service.isEncryptionEnabled()).isTrue();
    }

    @Test
    void upload_withR2_delegaPutObjectConBucketKeyYLongitud() {
        S3Client s3Client = mock(S3Client.class);
        BackupStorageService service = new BackupStorageService(s3Client);
        ReflectionTestUtils.setField(service, "bucket", "sgb-backups");

        service.upload("daily/backup.sql", "abc".getBytes());

        verify(s3Client).putObject(
                eq(PutObjectRequest.builder()
                        .bucket("sgb-backups")
                        .key("daily/backup.sql")
                        .contentLength(3L)
                        .build()),
                any(RequestBody.class));
    }

    @Test
    void download_withR2_delegaGetObjectYDevuelveBytes() {
        S3Client s3Client = mock(S3Client.class);
        BackupStorageService service = new BackupStorageService(s3Client);
        ReflectionTestUtils.setField(service, "bucket", "sgb-backups");
        ResponseBytes<GetObjectResponse> bytes = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                "contenido-r2".getBytes());
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(bytes);

        byte[] resultado = service.download("daily/backup.sql");

        assertThat(resultado).isEqualTo("contenido-r2".getBytes());
        verify(s3Client).getObjectAsBytes(GetObjectRequest.builder()
                .bucket("sgb-backups")
                .key("daily/backup.sql")
                .build());
    }

    @Test
    void delete_withR2_delegaDeleteObject() {
        S3Client s3Client = mock(S3Client.class);
        BackupStorageService service = new BackupStorageService(s3Client);
        ReflectionTestUtils.setField(service, "bucket", "sgb-backups");

        service.delete("daily/backup.sql");

        verify(s3Client).deleteObject(DeleteObjectRequest.builder()
                .bucket("sgb-backups")
                .key("daily/backup.sql")
                .build());
    }

    @Test
    void upload_withoutR2AndS3Url_usaBackupsLocalPorDefecto() {
        BackupStorageService service = new BackupStorageService(null);
        ReflectionTestUtils.setField(service, "storageUrl", "s3://bucket-remoto");

        service.upload("fallback.zip", "abc".getBytes());

        Path esperado = Path.of("./backups").resolve("fallback.zip");
        assertThat(Files.exists(esperado)).isTrue();
        assertThatCode(() -> Files.deleteIfExists(esperado)).doesNotThrowAnyException();
    }

    @Test
    void download_withEncryptionAndContenidoInvalido_envuelveErrorDeDescifrado() throws Exception {
        BackupStorageService service = new BackupStorageService(null);
        ReflectionTestUtils.setField(service, "storageUrl", tempDir.toString());
        ReflectionTestUtils.setField(service, "encryptionKey", "clave-de-prueba-para-tests");
        Files.write(tempDir.resolve("corrupto.zip"), "muy-corto".getBytes());

        assertThatThrownBy(() -> service.download("corrupto.zip"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al desencriptar respaldo");
    }
}
