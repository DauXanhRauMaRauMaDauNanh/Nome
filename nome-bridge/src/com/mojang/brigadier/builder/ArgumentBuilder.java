package com.mojang.brigadier.builder;

import com.mojang.brigadier.Command;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public abstract class ArgumentBuilder<S, T extends ArgumentBuilder<S, T>> {
    protected Command<S> command;
    protected Predicate<S> requirement = s -> true;
    protected final List<ArgumentBuilder<S, ?>> children = new ArrayList<>();

    protected abstract T getThis();

    public T executes(Command<S> command) {
        this.command = command;
        return getThis();
    }

    public T requires(Predicate<S> requirement) {
        this.requirement = requirement;
        return getThis();
    }

    public T then(ArgumentBuilder<S, ?> argument) {
        children.add(argument);
        return getThis();
    }

    public Command<S> getCommand() {
        return command;
    }
}
