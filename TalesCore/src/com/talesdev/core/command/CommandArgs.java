package com.talesdev.core.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command Framework - CommandArgs <br>
 * This class is passed to the command methods and contains various
 * utilities as well as the command info.
 *
 * @author minnymin3
 */
public class CommandArgs {

    private final CommandSender sender;
    private final org.bukkit.command.Command command;
    private final String label;
    private final String[] args;

    protected CommandArgs(CommandSender sender, org.bukkit.command.Command command, String label, String[] args,
                          int subCommand) {
        String[] modArgs = new String[args.length - subCommand];
        System.arraycopy(args, subCommand, modArgs, 0, args.length - subCommand);

        StringBuilder buffer = new StringBuilder();
        buffer.append(label);
        for (int x = 0; x < subCommand; x++) {
            buffer.append(".").append(args[x]);
        }
        String cmdLabel = buffer.toString();
        this.sender = sender;
        this.command = command;
        this.label = cmdLabel;
        this.args = modArgs;
    }

    /**
     * Gets the command sender
     *
     * @return sender
     */
    public CommandSender getSender() {
        return sender;
    }

    /**
     * Gets the original command object
     *
     * @return
     */
    public org.bukkit.command.Command getCommand() {
        return command;
    }

    /**
     * Gets the label including sub command labels of this command
     *
     * @return Something like 'test.subcommand'
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets all the arguments after the command's label. ie. if the command
     * label was test.subcommand and the arguments were subcommand foo foo,
     * it would only return 'foo foo' because 'subcommand' is part of the
     * command
     *
     * @return
     */
    public String[] getArgs() {
        return args;
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public Player getPlayer() {
        if (sender instanceof Player) {
            return (Player) sender;
        } else {
            return null;
        }
    }
}
