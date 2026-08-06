package com.example.router.service;

import com.example.router.config.RoutesProperties;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.Map;

/**
 * Определяет, в какой downstream-сервис отправить сообщение,
 * на основе XPath-правил из конфигурации (routes.*.matchXpath / matchValue).
 *
 * Пример правила в application.yml:
 *   routes:
 *     service-a:
 *       matchXpath: /sbapi/header/message/@type
 *       matchValue: "5000"
 *
 * Если у вас критерий маршрутизации другой (например, отдельный элемент
 * с кодом получателя, а не message/@type) — просто поменяйте matchXpath,
 * логика останется той же.
 */
@Service
public class RoutingService {

    private final RoutesProperties routesProperties;
    private final XPath xPath = XPathFactory.newInstance().newXPath();

    public RoutingService(RoutesProperties routesProperties) {
        this.routesProperties = routesProperties;
    }

    /**
     * Ищет маршрут, в чьём списке routes.*.iins встречается переданный ИИН
     * (точное совпадение строки).
     */
    public RouteMatch resolveRouteBySystemCode(String systemCode) {
        Map<String, RoutesProperties.RouteConfig> routes = routesProperties.getRoutes();
        if (routes == null || routes.isEmpty()) {
            throw new IllegalStateException("Маршруты не сконфигурированы (routes.*)");
        }

        for (Map.Entry<String, RoutesProperties.RouteConfig> entry : routes.entrySet()) {
            RoutesProperties.RouteConfig cfg = entry.getValue();
            if (cfg.getSystemCode() != null && cfg.getSystemCode().contains(systemCode)) {
                return new RouteMatch(entry.getKey(), cfg);
            }
        }

        throw new NoRouteFoundException("Не найден маршрут для ИИН " + systemCode);
    }

    public RouteMatch resolveRoute(Document doc) {
        Map<String, RoutesProperties.RouteConfig> routes = routesProperties.getRoutes();
        if (routes == null || routes.isEmpty()) {
            throw new IllegalStateException("Маршруты не сконфигурированы (routes.*)");
        }

        for (Map.Entry<String, RoutesProperties.RouteConfig> entry : routes.entrySet()) {
            RoutesProperties.RouteConfig cfg = entry.getValue();
            if (cfg.getMatchXpath() == null || cfg.getMatchValue() == null) {
                continue;
            }
            try {
                String actual = (String) xPath.evaluate(cfg.getMatchXpath(), doc, XPathConstants.STRING);
                if (cfg.getMatchValue().equals(actual)) {
                    return new RouteMatch(entry.getKey(), cfg);
                }
            } catch (XPathExpressionException e) {
                throw new IllegalStateException("Некорректный matchXpath для маршрута " + entry.getKey(), e);
            }
        }

        throw new NoRouteFoundException("Не найден маршрут для входящего XML");
    }

    public record RouteMatch(String routeName, RoutesProperties.RouteConfig config) {}

    public static class NoRouteFoundException extends RuntimeException {
        public NoRouteFoundException(String message) {
            super(message);
        }
    }
}
