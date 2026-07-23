# xml-router

XML-роутер (фильтр) на Spring Boot: принимает SOAP-конверт с вложенным
XML-документом внутри `<data>`, извлекает ИИН из вложенного XML,
определяет получателя по этому ИИН и отправляет исходящее сообщение
в формате sbapi (с auth-конвертом, хешированным SHA3-512).

## Формат входящего сообщения

```xml
<SOAP-ENV:Envelope ...>
  <SOAP-ENV:Body>
    <ns1:SendMessage>
      <request>
        <requestInfo>...</requestInfo>
        <requestData>
          <data><![CDATA[
            <?xml version="1.0" encoding="UTF-8"?>
            <cvRecruit><cv><iin>920929300914</iin>...</cv></cvRecruit>
          ]]></data>
        </requestData>
      </request>
    </ns1:SendMessage>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

Важный нюанс: содержимое `<data>` — это **отдельный, самостоятельный XML-документ**
(со своим `<?xml ...?>`), обёрнутый в `CDATA`. Он парсится **отдельно**, вторым
проходом, поэтому просто распарсить весь конверт как один документ не получится —
`NestedPayloadExtractor` делает это в два шага (см. ниже).

## Запуск

```bash
mvn spring-boot:run
```

Сервис поднимется на `http://localhost:8080`.

## Настройка маршрутов (по ИИН)

Файл `src/main/resources/application.yml`:

```yaml
routes:
  routes:
    service-a:
      url: https://service-a.example.com/api   # куда отправлять
      login: ts8.api                            # user в authdata
      password: "12345678"                      # открытый пароль, хешируется SHA3-512 перед отправкой
      iins:
        - "920929300914"
        - "111111111111"

    service-b:
      url: https://service-b.example.com/api
      login: ts8.api2
      password: "another-secret"
      iins:
        - "222222222222"
```

Каждый маршрут — это список конкретных ИИН, которые на него направляются
(точное совпадение строки). Если ИИН не найден ни в одном списке —
сервис вернёт `502 Bad Gateway` с сообщением "Не найден маршрут для ИИН ...".

## Пример запроса

```bash
curl -X POST http://localhost:8080/route \
  -H "Content-Type: application/xml" \
  --data-binary @incoming-sample.xml
```

`incoming-sample.xml` в корне проекта — готовый пример с ИИН `920929300914`,
который по умолчанию замаплен на `service-a`.

## Что происходит внутри

1. `IngressController` принимает SOAP-конверт, парсит его в DOM
   (namespace-aware, с защитой от XXE: `disallow-doctype-decl`).
2. `NestedPayloadExtractor`:
   - находит элемент `<data>` по local-name (без завязки на namespace),
   - берёт его текстовое содержимое (работает и для `CDATA`, и для
     экранированного текста),
   - парсит это содержимое как **отдельный** XML-документ,
   - достаёт из него `<iin>` (тоже по local-name).
3. `RoutingService.resolveRouteByIin(iin)` ищет маршрут, в чьём списке
   `routes.*.iins` есть этот ИИН.
4. `SbApiEnvelopeBuilder` собирает исходящий конверт:
   - хеширует пароль маршрута через `HashUtil.sha3_512Hex(...)`,
   - формирует `<authdata .../>`, кодирует в base64,
   - подставляет тот же хеш в атрибут `pwd`.
5. `OutboundClient` отправляет итоговый XML (`Content-Type: application/xml`)
   на `url` найденного маршрута через `RestTemplate`.

## Что нужно доработать под вашу специфику

- **Формат `arg[data]`** — сейчас туда экранированно попадает вложенный XML
  (`cvRecruit`) как есть. Если вам нужен другой payload (JSON, только часть
  полей и т.п.) — замените логику в `IngressController.route(...)`
  (помечено `// TODO`).
- **Источник ИИН → сервис** — сейчас список ИИН жёстко задан в
  `application.yml`. Если в реальности это динамический справочник
  (БД / внешний API), нужно заменить `RoutingService.resolveRouteByIin(...)`
  на обращение к этому источнику вместо статического конфига.
- **HTTP Basic Auth / доп. заголовки** для конкретных получателей — если
  нужны, добавьте в `OutboundClient.send(...)`.
- **`interface id` / `version` / `msg_type`** — сейчас захардкожены в
  `IngressController` (218124291 / 8 / "5000"). Вынесите в конфиг, если
  они должны отличаться по маршрутам.
- Старый вариант маршрутизации по XPath (`matchXpath`/`matchValue`,
  метод `RoutingService.resolveRoute(Document)`) оставлен в коде
  на случай, если понадобится маршрутизация не по ИИН, а по другому полю.
