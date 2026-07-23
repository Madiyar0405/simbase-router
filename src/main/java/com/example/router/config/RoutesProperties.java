package com.example.router.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Настройки маршрутов из application.yml (секция routes.*)
 *
 * Пример:
 * routes:
 *   service-a:
 *     url: https://service-a.example.com/api
 *     login: ts8.api
 *     password: real-secret
 *     matchXpath: /sbapi/header/message/@type
 *     matchValue: "5000"
 *     interfaceId: "11111"
 */
@ConfigurationProperties(prefix = "routes")
public class RoutesProperties {

    private Map<String, RouteConfig> routes;

    public Map<String, RouteConfig> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, RouteConfig> routes) {
        this.routes = routes;
    }

    public static class RouteConfig {
        /** URL получателя, куда будет отправлен итоговый sbapi-конверт */
        private String url;
        /** Логин (user) для authdata */
        private String login;
        /** Пароль в открытом виде — будет захеширован SHA3-512 перед отправкой */
        private String password;
        /** XPath-выражение, по которому проверяем входящий XML для выбора маршрута (опционально) */
        private String matchXpath;
        /** Значение, с которым сравнивается результат matchXpath (опционально) */
        private String matchValue;
        /** Список конкретных ИИН, для которых сообщение направляется на этот маршрут */
        private List<String> bin;
        /** InterfaceId системы Simbase, для которых сообщение направляется на этот маршрут */
        private String interfaceId ;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getMatchXpath() {
            return matchXpath;
        }

        public void setMatchXpath(String matchXpath) {
            this.matchXpath = matchXpath;
        }

        public String getMatchValue() {
            return matchValue;
        }

        public void setMatchValue(String matchValue) {
            this.matchValue = matchValue;
        }

        public List<String> getBin() {
            return bin;
        }

        public void setBin(List<String> bin) {
            this.bin = bin;
        }

        public String getInterfaceId() {
            return interfaceId;
        }

        public void setInterfaceId(String interfaceId) {
            this.interfaceId = interfaceId;
        }
    }
}
