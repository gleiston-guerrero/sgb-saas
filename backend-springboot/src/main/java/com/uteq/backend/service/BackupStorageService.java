package com.uteq.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class BackupStorageService {
    private final S3Client s3Client;
    @Value("${app.backup.r2.bucket:}") private String bucket;
    @Value("${app.backup.encryption-key:}") private String encryptionKey;
    @Value("${app.backup.storage-url:}") private String storageUrl;
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    /**
     * Constructor con el cliente S3 compatible (R2), opcional en local.
     *
     * @param s3Client cliente S3, null si no está configurado
     */
    public BackupStorageService(@org.springframework.beans.factory.annotation.Autowired(required = false) S3Client s3Client) { this.s3Client = s3Client; }
    /**
     * Indica si el almacenamiento remoto está disponible, es decir si hay cliente S3 y bucket configurado.
     * Cuando es falso los respaldos se guardan en el directorio local de respaldo.
     *
     * @return verdadero si los respaldos se suben al bucket remoto; falso si se usa disco local
     */
    public boolean isR2Configured() { return s3Client != null && bucket != null && !bucket.isBlank(); }
    /**
     * Indica si el cifrado AES-GCM de respaldos está habilitado por tener clave configurada.
     *
     * @return verdadero si los bytes se cifran antes de guardarse y se descifran al leerse
     */
    public boolean isEncryptionEnabled() { return encryptionKey != null && !encryptionKey.isBlank(); }
    /**
     * Guarda los bytes de un respaldo cifrándolos con AES-GCM cuando el cifrado está habilitado.
     * Sube el objeto al bucket remoto si está configurado; si no, lo escribe en el directorio local.
     *
     * @param key clave del objeto dentro del almacenamiento de respaldos
     * @param data bytes del respaldo a guardar, previos al cifrado
     * @throws RuntimeException si el guardado local en disco falla
     */
    public void upload(String key, byte[] data) {
        byte[] toStore = isEncryptionEnabled() ? encrypt(data) : data;
        if (isR2Configured()) {
            PutObjectRequest req = PutObjectRequest.builder().bucket(bucket).key(key).contentLength((long) toStore.length).build();
            s3Client.putObject(req, RequestBody.fromBytes(toStore));
        } else {
            Path base = resolveLocalBase();
            try { Files.createDirectories(base); Files.write(base.resolve(sanitize(key)), toStore); } catch (IOException e) { throw new RuntimeException("No se pudo guardar respaldo local", e); }
        }
    }
    /**
     * Lee los bytes de un respaldo desde el bucket remoto o el directorio local y los descifra
     * con AES-GCM cuando el cifrado está habilitado.
     *
     * @param key clave del objeto dentro del almacenamiento de respaldos
     * @return bytes originales del respaldo listos para descargar
     * @throws RuntimeException si el archivo local no puede leerse o el descifrado falla
     */
    public byte[] download(String key) {
        byte[] stored;
        if (isR2Configured()) { stored = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray(); }
        else { try { stored = Files.readAllBytes(resolveLocalBase().resolve(sanitize(key))); } catch (IOException e) { throw new RuntimeException("No se pudo leer respaldo local: " + key, e); } }
        return isEncryptionEnabled() ? decrypt(stored) : stored;
    }
    /**
     * Elimina el objeto de un respaldo del bucket remoto o del directorio local, según qué
     * almacenamiento esté configurado.
     *
     * @param key clave del objeto dentro del almacenamiento de respaldos
     * @throws RuntimeException si la eliminación local en disco falla
     */
    public void delete(String key) {
        if (isR2Configured()) s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        else { try { Files.deleteIfExists(resolveLocalBase().resolve(sanitize(key))); } catch (IOException e) { throw new RuntimeException("No se pudo eliminar respaldo local", e); } }
    }
    private Path resolveLocalBase() {
        if (storageUrl != null && !storageUrl.isBlank() && !storageUrl.startsWith("s3://")) return Paths.get(storageUrl);
        return Paths.get("./backups");
    }
    private String sanitize(String k) { return k.replace("/", "_").replace("\\", "_"); }
    private byte[] encrypt(byte[] plain) {
        try {
            byte[] kb = decodeKey();
            SecretKeySpec ks = new SecretKeySpec(kb, "AES");
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance(AES_GCM);
            c.init(Cipher.ENCRYPT_MODE, ks, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ct = c.doFinal(plain);
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv,0,out,0,iv.length);
            System.arraycopy(ct,0,out,iv.length,ct.length);
            return out;
        } catch (Exception e) { throw new RuntimeException("Error al encriptar respaldo", e); }
    }
    private byte[] decrypt(byte[] enc) {
        try {
            byte[] kb = decodeKey();
            SecretKeySpec ks = new SecretKeySpec(kb, "AES");
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(enc,0,iv,0,GCM_IV_LENGTH);
            byte[] ct = new byte[enc.length - GCM_IV_LENGTH];
            System.arraycopy(enc,GCM_IV_LENGTH,ct,0,ct.length);
            Cipher c = Cipher.getInstance(AES_GCM);
            c.init(Cipher.DECRYPT_MODE, ks, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return c.doFinal(ct);
        } catch (Exception e) { throw new RuntimeException("Error al desencriptar respaldo", e); }
    }
      private byte[] decodeKey() {
          String k = encryptionKey.trim();
          try { byte[] d = Base64.getDecoder().decode(k); if (d.length==32) return d; } catch (IllegalArgumentException ignored) {
              // best-effort: si no es Base64 se deriva del texto plano abajo
          }
          byte[] raw = k.getBytes(java.nio.charset.StandardCharsets.UTF_8);
          byte[] out = new byte[32];
          System.arraycopy(raw,0,out,0,Math.min(raw.length,32));
          return out;
      }
}
