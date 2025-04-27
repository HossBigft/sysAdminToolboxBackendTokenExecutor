package org.example.commands.picocli;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.exceptions.CommandFailedException;
import org.example.SysAdminToolboxBackendTokenExecutor;
import org.example.value_types.DomainName;
import picocli.CommandLine;

import java.util.Optional;

@CommandLine.Command(
        name = "get-test-mailbox",
        description = "Get test mailbox credentials for a domain"
)
public class GetTestMailboxCliCommand extends AbstractCliCommand {
    @CommandLine.Parameters(index = "0", description = "The domain to check")
    private String domain;


    public GetTestMailboxCliCommand(SysAdminToolboxBackendTokenExecutor parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        Optional<ObjectNode> mailCredentials;
        try {
            mailCredentials = getPleskService().getTestMailbox(new DomainName(domain));
        } catch (CommandFailedException e) {
            return error("Test mail creation failed: " + e.getMessage());
        }

        if (mailCredentials.isPresent()) {
            return success(mailCredentials.get().toString());
        } else {
            return error("Email for " + domain + " was not found");
        }
    }
}
