package com.uteq.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.uteq.backend.entity.User;
import com.uteq.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Genera y resuelve la credencial QR del usuario: identificación rápida en
 * ventanilla para agilizar préstamos (el ingreso manual sigue disponible).
 */
@Service
public class CredentialQrService {

    private static final int TAMANO_PX = 300;
    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String CREDENCIAL_NO_RECONOCIDA =
            "Credencial QR no reconocida o usuario inactivo.";

    private final UserRepository userRepo;

    /**
     * Constructor con el repositorio para resolver usuarios por correo o token de credencial.
     *
     * @param userRepo repositorio de usuarios
     */
    public CredentialQrService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Genera la imagen PNG de 300 px con la credencial QR del usuario autenticado para identificarlo
     * en ventanilla al prestar. El QR codifica solo el token, sin exponer datos personales si se pierde.
     *
     * @param authentication identidad autenticada del titular de la credencial
     * @return bytes de la imagen QR en formato PNG
     * @throws jakarta.persistence.EntityNotFoundException si el correo autenticado ya no existe en usuarios
     * @throws IllegalStateException si el generador del QR o la escritura de la imagen fallan
     */
    public byte[] generateImageQrOwn(Authentication authentication) {
        User user = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuario no encontrado: " + authentication.getName()));
        return generateImageQr(user);
    }

    /**
     * Codifica ÚNICAMENTE el token (UUID): el QR no expone datos personales si se pierde.
     */
    private byte[] generateImageQr(User user) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    user.getCredentialQrToken().toString(),
                    BarcodeFormat.QR_CODE, TAMANO_PX, TAMANO_PX);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            // Si pasa, es un problema del entorno (encoder/IO), no del dato.
            throw new IllegalStateException("No se pudo generar el código QR.", ex);
        }
    }

    /**
     * Resuelve el usuario dueño de un token de credencial QR escaneado en ventanilla.
     * Solo acepta titulares en estado ACTIVO; un token ajeno se reporta igual que uno inexistente.
     *
     * @param token token UUID leído del QR presentado por el lector
     * @return el usuario titular del token en estado ACTIVO
     * @throws jakarta.persistence.EntityNotFoundException si el token no existe o el titular no está activo
     */
    public User resolveByToken(UUID token) {
        User user = userRepo.findByCredentialQrToken(token)
                .orElseThrow(() -> new EntityNotFoundException(CREDENCIAL_NO_RECONOCIDA));
        if (!ESTADO_ACTIVO.equals(user.getStatus().getName())) {
            throw new EntityNotFoundException(CREDENCIAL_NO_RECONOCIDA);
        }
        return user;
    }
}
