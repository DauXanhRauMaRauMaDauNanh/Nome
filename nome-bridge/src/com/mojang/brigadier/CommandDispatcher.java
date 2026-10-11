package com.mojang.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.HashMap;
import java.util.Map;

public class CommandDispatcher<S> {
    private final Map<String, LiteralArgumentBuilder<S>> rootNodes = new HashMap<>();

    public void register(LiteralArgumentBuilder<S> command) {
        rootNodes.put(command.getLiteral(), command);
    }

    public int execute(String commandLine, S source) throws Exception {
        if (commandLine == null || commandLine.trim().isEmpty()) return 0;
        String[] parts = commandLine.trim().split("\\s+");
        String name = parts[0];
        if (name.startsWith("/")) name = name.substring(1);

        LiteralArgumentBuilder<S> node = rootNodes.get(name);
        if (node != null && node.getCommand() != null) {
            CommandContext<S> context = new CommandContext<>(source);
            return node.getCommand().run(context);
        }
        return 0;
    }
}
