package org.example.commands.dns;

import org.example.commands.Command;
import org.example.value_types.DomainName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GetZoneMaster implements Command<String> {
    private static final Path ZONEFILE_PATH_BIND = Paths.get("/var/opt/isc/scls/isc-bind/zones/_default.nzf");
    private static final Path PLESK_BIND_ZONE_DIR = Paths.get("/var/named/run-root/");
    private static final Pattern IP_REGEX = Pattern.compile("((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}");
    private final DomainName domain;

    public GetZoneMaster(DomainName domainName) {
        this.domain = domainName;
    }

    @Override
    public Optional<String> execute() throws IOException {
        Optional<String> result = getZoneMasterIp(domain.name());

        if (result.isEmpty()) {
            result = getZoneMasterIpFromPleskBind(domain.name());
        }

        return result;
    }

    private Optional<String> getZoneMasterIp(String domainName) throws IOException {
        String loweredDomain = domainName.toLowerCase();
        Pattern domainPattern = Pattern.compile("\\b" + Pattern.quote(loweredDomain) + "\\b");

        if (Files.exists(ZONEFILE_PATH_BIND)) {
            try (Stream<String> lines = Files.lines(ZONEFILE_PATH_BIND)) {
                return lines
                        .filter(line -> domainPattern.matcher(line).find())
                        .flatMap(GetZoneMaster::extractIps)
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    private Optional<String> getZoneMasterIpFromPleskBind(String domainName) throws IOException {
        String loweredDomain = domainName.toLowerCase();
        Pattern nsPattern = Pattern.compile("\\bns\\d+\\." + Pattern.quote(loweredDomain) + "\\b");

        Path specificZoneFilePath = PLESK_BIND_ZONE_DIR.resolve(Paths.get("var", loweredDomain));

        if (Files.exists(specificZoneFilePath) && Files.isRegularFile(specificZoneFilePath)) {
            try (Stream<String> lines = Files.lines(specificZoneFilePath)) {
                return lines
                        .filter(line -> nsPattern.matcher(line).find())
                        .filter(line -> line.contains("IN A"))
                        .flatMap(GetZoneMaster::extractIps)
                        .findFirst();
            }
        }

        return Optional.empty();
    }

    private static Stream<String> extractIps(String line) {
        Matcher matcher = IP_REGEX.matcher(line);
        Stream.Builder<String> builder = Stream.builder();
        while (matcher.find()) {
            builder.add(matcher.group());
        }
        return builder.build();
    }
}