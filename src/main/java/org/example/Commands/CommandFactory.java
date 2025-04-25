package org.example.Commands;

import org.example.Interfaces.Command;
import org.example.ServiceCommand;
import org.example.ValueTypes.CommandDTO;

public interface CommandFactory {



    Command<?> createCommand(CommandDTO cmdto);
}
