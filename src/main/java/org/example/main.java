package org.example;

import org.example.operations.OperationResult;
import org.example.picocli.subcommands.ExecuteSubCommand;
import org.example.picocli.subcommands.HealthCheckSubCommand;
import org.example.picocli.subcommands.InitSubCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "sysadmintoolbox", description = "Safe root wrapper for executing Plesk and Bind administration commands. github.com/HossBigft/sysAdminToolboxBackendTokenExecutor", version = "0.3.3", mixinStandardHelpOptions = true)
public class main implements Callable<Integer> {


    public static void main(String[] args) {

        main app = new main();
        CommandLine commandLine = new CommandLine(app);
        commandLine.addSubcommand(new ExecuteSubCommand(app));
        commandLine.addSubcommand(new InitSubCommand(app));
        commandLine.addSubcommand(new HealthCheckSubCommand(app));

        if (args.length == 0) {
            String sshOriginalCommand = System.getenv("SSH_ORIGINAL_COMMAND");
            if (sshOriginalCommand == null) {
                System.err.println("Error: SSH_ORIGINAL_COMMAND is missing");
                System.exit(1);
            }
            try {
                args = tokenizeSSHORIGINALCOMMAND(sshOriginalCommand);
            } catch (IOException e) {
                System.err.println("Error parsing SSH_ORIGINAL_COMMAND: " + e.getMessage());
                System.exit(1);
            }
        }

        try {
            commandLine.parseArgs(args);
        } catch (CommandLine.UnmatchedArgumentException e) {
            PrintWriter err = commandLine.getErr();
            System.out.println(OperationResult.failure(OperationResult.ExecutionStatus.UNPROCCESIBLE_ENTITY,
                    "Unknown command: " + Arrays.toString(args)).toPrettyJson());
            commandLine.usage(err);
            return;
        }

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    private static String[] tokenizeSSHORIGINALCOMMAND(String cmdString) throws IOException {
        StreamTokenizer tok = new StreamTokenizer(new StringReader(cmdString));
        tok.resetSyntax();
        tok.wordChars(' ', 255);
        tok.whitespaceChars(0, ' ');
        tok.quoteChar('"');
        tok.quoteChar('\'');
        tok.commentChar('#');

        List<String> arguments = new ArrayList<>();
        while (tok.nextToken() != StreamTokenizer.TT_EOF) {
            arguments.add(tok.sval);
        }
        return arguments.toArray(String[]::new);
    }


    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }
}