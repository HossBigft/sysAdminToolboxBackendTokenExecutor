package org.example.commands.picocli;

import org.example.commands.CommandRequest;
import org.example.commands.core.PleskCommandExecutorFactory;
import org.example.SysAdminToolboxBackendTokenExecutor;
import org.example.value_types.Token;
import org.example.token_handler.TokenProcessor;
import picocli.CommandLine;

import javax.naming.CommunicationException;

@CommandLine.Command(
        name = "execute",
        description = "Executes command from signed token"
)
public class ExecuteCliCommand extends AbstractCliCommand {
    @CommandLine.Parameters(index = "0", description = "The signed token")
    private String rawToken;

    public ExecuteCliCommand(SysAdminToolboxBackendTokenExecutor parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        try {
            Token token = Token.fromJson(rawToken);
            CommandRequest command = new TokenProcessor()
                    .processToken(token)
                    .orElseThrow(CommunicationException::new);
            System.out.println("Extracted command " + command);
            System.out.println(new PleskCommandExecutorFactory().build(command).execute());
            return 0;
        } catch (Exception e) {
            System.out.println("Failed to parse token" + rawToken + " ");
            e.printStackTrace();
            return 1;
        }
    }
}
