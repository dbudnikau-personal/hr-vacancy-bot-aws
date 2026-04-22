package com.hrbot.parser;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ParserRegistry {

    private final Map<String, SiteParser> parsers;

    public ParserRegistry(List<SiteParser> parserList) {
        this.parsers = parserList.stream()
                .collect(Collectors.toMap(SiteParser::getSiteKey, Function.identity()));
    }

    public SiteParser getParser(String siteKey) {
        SiteParser parser = parsers.get(siteKey);
        if (parser == null) throw new IllegalArgumentException("No parser for site: " + siteKey);
        return parser;
    }

    public List<String> availableSites() {
        return List.copyOf(parsers.keySet());
    }
}
