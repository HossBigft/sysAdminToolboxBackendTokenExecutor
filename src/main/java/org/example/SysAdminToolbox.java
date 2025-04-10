package org.example;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

@Command(name = "sysadmintoolbox",
        description = "Executes sudo commands on server",
        mixinStandardHelpOptions = true
)
public class SysAdminToolbox implements Callable<Integer> {
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("^(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.(?!-)[A-Za-z0-9.-]{2,}$");

    @Parameters(index = "0", description = "The email login (before the @).")
    private String testMailLogin;

    @Parameters(index = "1", description = "The domain to check.")
    private String domain;

    @Override
    public Integer call() throws Exception {
        if (!isValidDomain(domain)) {
            System.err.println("Error: Invalid domain format.");
            return 1;
        }

        ProcessBuilder builder = new ProcessBuilder("/usr/local/psa/admin/bin/mail_auth_view");
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines()
                    .filter(line -> line.contains(testMailLogin + "@" + domain))
                    .map(line -> line.replaceAll("\\s", "")) // remove all whitespace
                    .map(line -> {
                        int index = line.indexOf('|');
                        return index >= 0 ? line.split("\\|")[3] : "";
                    })
                    .forEach(System.out::println);
        }

        return 0;
    }

    private boolean isValidDomain(String domain) {
        return DOMAIN_PATTERN.matcher(domain).matches();
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SysAdminToolbox()).execute(args);
        System.exit(exitCode);
    }
}
