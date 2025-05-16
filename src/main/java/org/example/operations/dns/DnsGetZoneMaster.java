package org.example.operations.dns;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.operations.Operation;
import org.example.operations.OperationResult;
import org.example.value_types.DomainName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DnsGetZoneMaster implements Operation {
    private static final Path ZONEFILE_PATH_BIND = Paths.get("/var/opt/isc/scls/isc-bind/zones/_default.nzf");
    private static final Path PLESK_BIND_ZONE_DIR = Paths.get("/var/named/run-root/");
    private static final Pattern IP_REGEX = Pattern.compile("((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}");
    private final DomainName domain;

    public DnsGetZoneMaster(DomainName domainName) {
        this.domain = domainName;
    }

    @Override
    public OperationResult execute() {
        ObjectMapper om = new ObjectMapper();
        ObjectNode zoneData = om.createObjectNode();
        Optional<String> result = getZoneMasterIp(domain.name());

        if (result.isEmpty()) {
            result = getZoneMasterIpFromPleskBind(domain.name());
        }
        if (result.isEmpty()) {
            return OperationResult.notFound(String.format("BIND DNS zone for domain %s not found.", domain));
        }
        zoneData.put("domain_name", domain.name());
        zoneData.put("zonemaster_ip", result.get());

        return OperationResult.success(Optional.of(zoneData));
    }

    private Optional<String> getZoneMasterIp(String domainName) {
        String loweredDomain = domainName.toLowerCase();
        Pattern domainPattern = Pattern.compile("\\b" + Pattern.quote(loweredDomain) + "\\b");
        try {
            if (Files.exists(ZONEFILE_PATH_BIND)) {
                try (Stream<String> lines = Files.lines(ZONEFILE_PATH_BIND)) {
                    return lines
                            .filter(line -> domainPattern.matcher(line).find())
                            .flatMap(DnsGetZoneMaster::extractIps)
                            .findFirst();
                }
            }
        } catch (IOException e) {
            getLogger().errorEntry().message("Bind zone file not found.").field("Path", ZONEFILE_PATH_BIND).exception(e)
                    .log();
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<String> getZoneMasterIpFromPleskBind(String domainName) {
        String loweredDomain = domainName.toLowerCase();
        Pattern nsPattern = Pattern.compile("\\bns\\d+\\." + Pattern.quote(loweredDomain) + "\\b");

        Path specificZoneFilePath = PLESK_BIND_ZONE_DIR.resolve(Paths.get("var", loweredDomain));
        try {
            if (Files.exists(specificZoneFilePath) && Files.isRegularFile(specificZoneFilePath)) {
                try (Stream<String> lines = Files.lines(specificZoneFilePath)) {
                    return lines
                            .filter(line -> nsPattern.matcher(line).find())
                            .filter(line -> line.contains("IN A"))
                            .flatMap(DnsGetZoneMaster::extractIps)
                            .findFirst();
                }
            }
        } catch (IOException e) {
            getLogger().errorEntry().message("Plesk bind zone file not found.").field("Path", specificZoneFilePath)
                    .exception(e)
                    .log();
            return Optional.empty();
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

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }
}