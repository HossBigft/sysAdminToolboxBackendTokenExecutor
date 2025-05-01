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
    private static final Path ZONEFILE_PATH = Paths.get("/var/opt/isc/scls/isc-bind/zones/_default.nzf");
    private static final Pattern IP_REGEX = Pattern.compile("((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}");
    private final DomainName domain;

    public GetZoneMaster(DomainName domainName) {
        this.domain = domainName;
    }

    @Override
    public Optional<String> execute() throws IOException {
        return getZoneMasterIp(domain.name());
    }

    private Optional<String> getZoneMasterIp(String domainName) throws IOException {
        String loweredDomain = domainName.toLowerCase();

        try (Stream<String> lines = Files.lines(ZONEFILE_PATH)) {
            return lines
                    .filter(line -> line.contains(loweredDomain)) // grep -F
                    .flatMap(GetZoneMaster::extractIps)          // grep -Po
                    .findFirst();                                 // head -n1
        }
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
