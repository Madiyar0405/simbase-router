package com.example.router.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Собирает исходящий XML-конверт вида:
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <sbapi>
 *   <header>
 *     <interface id="..." version="..."/>
 *     <message id="..." ignore_id="yes" type="..." created="..."/>
 *     <error id="0"/>
 *     <auth pwd="hash">BASE64(<authdata .../>)</auth>
 *   </header>
 *   <body><function name="f_send_msg"><arg name="data">...</arg></function></body>
 * </sbapi>
 *
 * Подтверждено референсным скриптом: pwd — литеральная строка "hash"
 * (не вычисленный хеш!), authdata кодируется в base64.
 */
@Component
public class SbApiEnvelopeBuilder {

    private static final DateTimeFormatter CREATED_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(java.time.ZoneOffset.UTC);

    private final HashUtil hashUtil;
    private final AtomicLong msgIdSequence = new AtomicLong(1);

    public SbApiEnvelopeBuilder(HashUtil hashUtil) {
        this.hashUtil = hashUtil;
    }

    public String build(BuildRequest req) {
        String createdAt = CREATED_FORMATTER.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        String passwordHash = hashUtil.sha3_512Hex(req.password());

        String authData = "<authdata msg_id=\"%d\" user=\"%s\" password=\"%s\" msg_type=\"%s\" user_ip=\"%s\" />"
                .formatted(req.msgId(), escape(req.login()), passwordHash, escape(req.msgType()), escape(req.userIp()));

        String authDataBase64 = Base64.getEncoder()
                .encodeToString(authData.getBytes(StandardCharsets.UTF_8));

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<sbapi><header>"
                + "<interface id=\"%d\" version=\"%d\"/>".formatted(req.interfaceId(), req.version())
                + "<message id=\"%d\" ignore_id=\"yes\" type=\"%s\" created=\"%s\"/>"
                        .formatted(req.msgId(), escape(req.msgType()), createdAt)
                + "<error id=\"0\"/>"
                + "<auth pwd=\"hash\">%s</auth>".formatted(authDataBase64)
                + "</header><body>"
                + "<function name=\"f_send_msg\"><arg name=\"data\">%s</arg></function>".formatted(req.dataPayload())
                + "</body></sbapi>";
    }

    public long nextMsgId() {
        return msgIdSequence.getAndIncrement();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Данные для сборки конверта.
     *
     * @param dataPayload содержимое arg[data] — уже готовая строка (JSON/XML/текст),
     *                    экранирование под конкретный формат делается заранее вызывающей стороной.
     */
    public record BuildRequest(
            long interfaceId,
            int version,
            long msgId,
            String msgType,
            String login,
            String password,
            String userIp,
            String dataPayload
    ) {}
}
