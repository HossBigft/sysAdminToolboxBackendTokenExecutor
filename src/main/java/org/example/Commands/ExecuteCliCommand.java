package org.example.Commands;

import org.example.SysAdminToolboxBackendTokenExecutor;
import org.example.Utils.TokenValidator;
import org.example.ValueTypes.Token;
import picocli.CommandLine;

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
            if (TokenValidator.isValid(token)) {
                System.out.println("Extracted command " + token.command());
            }
            return 0;
        } catch (Exception e) {
            System.out.println("Failed to parse token" + rawToken + " ");
            e.printStackTrace();
            return 1;
        }
    }
}
