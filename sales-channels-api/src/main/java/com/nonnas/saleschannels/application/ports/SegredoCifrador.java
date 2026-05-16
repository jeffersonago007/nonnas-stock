package com.nonnas.saleschannels.application.ports;

/**
 * Port para cifrar/decifrar o {@code clientSecret} antes de salvar em
 * {@code canais_credenciais.client_secret_cifrado}.
 *
 * <p>Implementação default {@link com.nonnas.saleschannels.infrastructure.crypto.SegredoCifradorBase64}
 * usa Base64 (POC — não é cifragem real, só esconde do log casual). A
 * implementação real (AES-256-GCM via {@code CryptoService} de T16) será
 * cabeada em {@code app/} via @Primary, sobrescrevendo este default. A
 * separação preserva o isolamento de bounded contexts — sales-channels-api
 * não importa identity diretamente.
 */
public interface SegredoCifrador {

    /** Recebe segredo em claro, devolve string opaca para persistência. */
    String cifrar(String emClaro);

    /** Recebe a string opaca persistida, devolve o segredo em claro. */
    String decifrar(String cifrado);
}
