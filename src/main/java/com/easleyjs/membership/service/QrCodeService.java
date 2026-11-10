package com.easleyjs.membership.service;

import io.github.simonscholz.qrcode.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.util.UUID;


/*


var qrCode = qrCodeApi.createQrCodeImage(config);
 */

@Service
public class QrCodeService {
    public byte[] generateQr(
            UUID memberId
    ) {
        final QrCodeApi qrCodeApi = QrCodeFactory.createQrCodeApi();

        try (var logoStream = new ClassPathResource("qr/member-logo.png").getInputStream()) {
            var logo = ImageIO.read(logoStream);
            if (logo == null) {
                throw new IllegalStateException("Could not read logo PNG");
            }

            var config = new QrCodeConfig.Builder(memberId.toString())
                    .qrCodeSize(480)
                    .qrCodeDotStyler(QrCodeDotShape.CIRCLE)
                    .qrPositionalSquaresConfig(new QrPositionalSquaresConfig(true, 0.25))
                    .qrBorderConfig(Color.BLACK, 0.04, 0.12)
                    .qrLogoConfig(logo, 0.30, Color.WHITE)
                    .build();

            var qrCode = qrCodeApi.createQrCodeImage(config);
            var png = new ByteArrayOutputStream();
            ImageIO.write(qrCode, "png", png);
            return png.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not generate member QR code", e);
        }
    }
}
