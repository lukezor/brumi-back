package br.com.brumi.service;

import org.springframework.web.multipart.MultipartFile;

/** Um arquivo que ja passou por todas as validacoes (ValidadorImagens) e esta pronto para ser gravado. */
public record ImagemValidada(MultipartFile arquivo, TipoImagem tipo) {
}
