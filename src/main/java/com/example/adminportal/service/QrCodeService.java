package com.example.adminportal.service;

import org.springframework.stereotype.Service;
import io.github.simonscholz.qrcode.QrCodeApi;
import io.github.simonscholz.qrcode.QrCodeConfig;
import io.github.simonscholz.qrcode.QrCodeFactory;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.util.UUID;

@Service
public class QrCodeService {
    public byte[] generateQr(
            UUID memberId
    ) {
        final QrCodeApi qrCodeApi = QrCodeFactory.createQrCodeApi();
        final var qrCode = qrCodeApi.createQrCodeImage(new QrCodeConfig(memberId.toString(), 480));

        final var png = new ByteArrayOutputStream();
        try {
            ImageIO.write(qrCode, "png", png);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not encode QR code as PNG", e);
        }
        return png.toByteArray();
    }
}
