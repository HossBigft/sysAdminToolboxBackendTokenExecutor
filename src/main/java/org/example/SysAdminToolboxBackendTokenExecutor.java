package org.example;

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
    @Parameters(index = "1", description = "The domain to check.")
    private int id;


    public static void main(String[] args) {
        int exitCode = new CommandLine(new SysAdminToolboxBackendTokenExecutor()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
//        if (!isDomain.test(domain)) {
//            System.err.println("Error: Invalid domain format.");
//            return 1;
//        }
        Optional<String> mailCredentials = Optional.empty();
        try {
            mailCredentials = new PleskService().pleskGetSubscriptionLoginLinkBySubscriptionId(id, domain);
        } catch (ShellUtils.CommandFailedException e) {
            System.out.println("Test mail creation failed with " + e);
            return 1;
        } catch (SQLException e){
            e.printStackTrace();
        }
        mailCredentials.ifPresentOrElse(creds -> System.out.println(String.join("", creds)),
                () -> System.out.println("Email for " + domain + " was not found"));

        return 0;
    }

}
