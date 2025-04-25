package org.example.Commands.factory;

import org.example.Commands.CommandFactory;
import org.example.Commands.Plesk.PleskGetLoginLinkCommand;
import org.example.Interfaces.Command;
import org.example.ServiceCommand;
import org.example.ValueTypes.CommandDTO;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class PleskCommandFactory implements CommandFactory {
    private static final Map<String, Function<CommandDTO, Command<?>>> factoryMap = Map.of(
            ServiceCommand.Plesk.GET_LOGIN_LINK, PleskGetLoginLinkCommand::new
    );

    @Override
    public Command<?> createCommand(CommandDTO commandDTO) {
        // Look up the command from the factory map and create it using the provided CommandDTO
        Function<CommandDTO, Command<?>> commandCreator = factoryMap.get(command.name());
        if (commandCreator != null) {
            return commandCreator.apply(commandDTO); // Pass CommandDTO to the constructor of the command
        } else {
            throw new IllegalArgumentException("Invalid command type: " + command);
        }
    }
}
