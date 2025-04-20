package org.example;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.Exceptions.CommandFailedException;
import org.example.ValueTypes.DomainName;
import org.example.ValueTypes.LinuxUsername;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.Callable;


@Command(name = "sysadmintoolbox", description = "Executes sudo commands on server", mixinStandardHelpOptions = true)
public class SysAdminToolboxBackendTokenExecutor implements Callable<Integer> {

    @Parameters(index = "0", description = "The domain to check.")
    private String domain;



    public static void main(String[] args) {
        int exitCode = new CommandLine(new SysAdminToolboxBackendTokenExecutor()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {

        Optional<ObjectNode> mailCredentials = Optional.empty();
        try {
            mailCredentials = new PleskService().plesk_get_testmail_credentials( new DomainName(domain));
        } catch (CommandFailedException e) {
            System.out.println("Test mail creation failed with " + e);
            return 1;
        }
        mailCredentials.ifPresentOrElse(
                System.out::println,
                () -> System.out.println("Email for " + domain + " was not found")
        );

        return 0;
    }

}
