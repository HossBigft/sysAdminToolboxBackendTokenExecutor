package org.example.commands.picocli;

import org.example.SysAdminToolboxBackendTokenExecutor;
import org.example.commands.CommandRequest;
import org.example.commands.core.AvailableCommand;
import org.example.commands.core.NsExecutorFactory;
import org.example.commands.core.PleskCommandExecutorFactory;
import org.example.token_handler.TokenProcessor;
import org.example.value_types.Token;
import picocli.CommandLine;

import javax.naming.CommunicationException;
import java.util.Base64;

@CommandLine.Command(
        name = "execute",
        description = "Executes command from signed token"
)
public class ExecuteCliCommand extends AbstractCliCommand {
    @CommandLine.Parameters(index = "0", description = "The signed token")
    private String encodedJson;

    public ExecuteCliCommand(SysAdminToolboxBackendTokenExecutor parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        try {
            String rawJson = new String(Base64.getDecoder().decode(encodedJson));
            Token token = Token.fromJson(rawJson);
            CommandRequest command = new TokenProcessor()
                    .processToken(token)
                    .orElseThrow(CommunicationException::new);
            System.out.println("Extracted command " + command);
            switch (command.commandName()) {
                case AvailableCommand.Plesk pleskCommand ->
                        System.out.println(new PleskCommandExecutorFactory().build(command)
                                .execute()
                                .map(Object::toString)
                                .orElse(""));
                case AvailableCommand.NS nsCommand -> System.out.println(new NsExecutorFactory().build(command)
                        .execute()
                        .map(Object::toString)
                        .orElse(""));
            }

            return 0;
        } catch (Exception e) {
            System.out.println("Failed to parse token" + encodedJson + " ");
            e.printStackTrace();
            return 1;
        }
    }
}
