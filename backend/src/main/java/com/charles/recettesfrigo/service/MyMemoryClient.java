package com.charles.recettesfrigo.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client de l'API de traduction gratuite MyMemory (https://mymemory.translated.net),
 * sans compte ni cle. Limites : 500 octets par requete, et un quota quotidien de
 * caracteres (5 000 en anonyme, 50 000 si une adresse email est fournie via la
 * variable d'environnement MYMEMORY_EMAIL - aucune inscription necessaire).
 * <p>
 * Pour economiser les requetes, les textes courts (titre, noms d'ingredients,
 * quantites) sont regroupes, un par ligne, dans une meme requete ; les textes longs
 * (instructions) sont decoupes par phrases.
 */
@Component
public class MyMemoryClient {

    /** Marge sous la limite de 500 octets par requete. */
    static final int MAX_BYTES = 450;

    private final RestClient restClient;
    private final String email;

    public MyMemoryClient(@Value("${app.translation.email:}") String email, RestClient.Builder builder) {
        this.email = email == null ? "" : email.trim();
        this.restClient = builder.baseUrl("https://api.mymemory.translated.net").build();
    }

    private record Response(ResponseData responseData, Object responseStatus) {}

    private record ResponseData(String translatedText) {}

    /** Traduit les textes de l'anglais vers le francais, dans le meme ordre (textes vides inchanges). */
    public List<String> translateToFrench(List<String> texts) {
        List<String> result = new ArrayList<>(Collections.nCopies(texts.size(), null));
        List<Integer> batch = new ArrayList<>();
        int batchBytes = 0;
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text == null || text.isBlank()) {
                result.set(i, text);
            } else if (text.contains("\n") || bytes(text) > MAX_BYTES) {
                result.set(i, translateLong(text));
            } else {
                if (batchBytes + bytes(text) + 1 > MAX_BYTES) {
                    translateBatch(texts, batch, result);
                    batch.clear();
                    batchBytes = 0;
                }
                batch.add(i);
                batchBytes += bytes(text) + 1;
            }
        }
        translateBatch(texts, batch, result);
        return result;
    }

    /** Textes courts, un par ligne ; si le nombre de lignes change a la traduction, un par un. */
    private void translateBatch(List<String> texts, List<Integer> batch, List<String> result) {
        if (batch.isEmpty()) {
            return;
        }
        String joined = String.join("\n", batch.stream().map(texts::get).toList());
        String[] lines = request(joined).split("\n", -1);
        for (int k = 0; k < batch.size(); k++) {
            int index = batch.get(k);
            result.set(index, lines.length == batch.size() ? lines[k].trim() : request(texts.get(index)));
        }
    }

    /** Texte long (instructions) : ligne par ligne, chaque ligne en morceaux de phrases. */
    private String translateLong(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            if (line.isBlank()) {
                lines.add("");
                continue;
            }
            List<String> chunks = new ArrayList<>();
            for (String chunk : chunks(line.trim())) {
                chunks.add(request(chunk));
            }
            lines.add(String.join(" ", chunks));
        }
        return String.join("\n", lines);
    }

    /** Phrases regroupees en morceaux de MAX_BYTES au plus (une phrase trop longue est coupee par mots). */
    static List<String> chunks(String line) {
        List<String> pieces = new ArrayList<>();
        for (String sentence : line.split("(?<=[.!?;])\\s+")) {
            if (bytes(sentence) <= MAX_BYTES) {
                pieces.add(sentence);
            } else {
                pieces.addAll(pack(List.of(sentence.split("\\s+"))));
            }
        }
        return pack(pieces);
    }

    private static List<String> pack(List<String> pieces) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String piece : pieces) {
            if (current.length() > 0 && bytes(current + " " + piece) > MAX_BYTES) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            current.append(current.length() > 0 ? " " : "").append(piece);
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private String request(String text) {
        Response response = restClient
                .get()
                .uri(builder -> {
                    builder.path("/get").queryParam("q", "{q}").queryParam("langpair", "{langpair}");
                    if (!email.isEmpty()) {
                        builder.queryParam("de", "{de}");
                    }
                    return builder.build(Map.of("q", text, "langpair", "en|fr", "de", email));
                })
                .retrieve()
                .body(Response.class);
        String translated = response == null || response.responseData() == null
                ? null
                : response.responseData().translatedText();
        // Quota du jour epuise ou erreur : MyMemory renvoie un message a la place de la traduction
        if (response == null
                || !"200".equals(String.valueOf(response.responseStatus()))
                || translated == null
                || translated.toUpperCase(Locale.ROOT).startsWith("MYMEMORY WARNING")
                || translated.toUpperCase(Locale.ROOT).contains("QUERY LENGTH LIMIT")) {
            throw new IllegalStateException("Traduction MyMemory indisponible : " + translated);
        }
        return Parser.unescapeEntities(translated, false);
    }

    private static int bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
