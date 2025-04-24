package org.example;

import org.example.Commands.ExecuteCliCommand;
import org.example.Commands.GetLoginLinkCliCommand;
import org.example.Commands.GetSubscriptionInfoCliCommand;
import org.example.Commands.GetTestMailboxCliCommand;
import org.example.Utils.Logging.core.LogLevel;
import org.example.Utils.Logging.facade.LogManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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
    @Option(names = {"--verbose"}, description = "Enable debug output", scope = CommandLine.ScopeType.INHERIT)
    boolean verbose;

    public SysAdminToolboxBackendTokenExecutor() {
        this.pleskService = new PleskService();
    }

    public static void main(String[] args) {

        SysAdminToolboxBackendTokenExecutor app = new SysAdminToolboxBackendTokenExecutor();
        CommandLine commandLine = new CommandLine(app);


        commandLine.addSubcommand(new GetTestMailboxCliCommand(app));
        commandLine.addSubcommand(new GetLoginLinkCliCommand(app));
        commandLine.addSubcommand(new GetSubscriptionInfoCliCommand(app));
        commandLine.addSubcommand(new ExecuteCliCommand(app));
        commandLine.parseArgs(args);

        if (app.debug) {
            LogManager.Builder.config().globalLogLevel(LogLevel.DEBUG);
        }
        if (app.verbose) {
            LogManager.Builder.config().setVerbose();
        }

        int exitCode = commandLine.execute(args);

        System.exit(exitCode);
    }

    public PleskService getPleskService() {
        return pleskService;
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

}