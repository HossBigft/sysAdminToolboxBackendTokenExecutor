package org.example;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.Commands.GetLoginLinkCliCommand;
import org.example.Commands.GetSubscriptionInfoCliCommand;
import org.example.Commands.GetTestMailboxCliCommand;
import org.example.Exceptions.CommandFailedException;
import org.example.Utils.Logging.LogManager;
import org.example.ValueTypes.DomainName;
import org.example.ValueTypes.LinuxUsername;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "sysadmintoolbox",
        description = "Safe root wrapper for executing system administration commands",
        mixinStandardHelpOptions = true
)
public class SysAdminToolboxBackendTokenExecutor implements Callable<Integer> {
    private final PleskService pleskService;

    @Option(names = {"--debug"}, description = "Enable debug output", scope = CommandLine.ScopeType.INHERIT)
    boolean debug;

    public SysAdminToolboxBackendTokenExecutor() {
        this.pleskService = new PleskService();
    }

    public PleskService getPleskService() {
        return pleskService;
    }

    public static void main(String[] args) {

        SysAdminToolboxBackendTokenExecutor app = new SysAdminToolboxBackendTokenExecutor();
        CommandLine commandLine = new CommandLine(app);


        commandLine.addSubcommand(new GetTestMailboxCliCommand(app));
        commandLine.addSubcommand(new GetLoginLinkCliCommand(app));
        commandLine.addSubcommand(new GetSubscriptionInfoCliCommand(app));
        commandLine.parseArgs(args);

        if (app.debug){
            LogManager.Builder.config().globalLogLevel(LogManager.LogLevel.DEBUG);
        }

        int exitCode = commandLine.execute(args);

        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

}