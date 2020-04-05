package org.perudevteam.parser.grammar;

import io.vavr.Function1;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import org.perudevteam.dynamic.Dynamic;

import java.util.Objects;

/**
 * This class represents a production which can be used to build some result.
 *
 * @param <NT> The non terminal enum type of the production.
 * @param <T> the terminal enum type of the production.
 * @param <R> The result which can be generated using this production.
 */
public abstract class AttrProduction<NT extends Enum<NT>, T extends Enum<T>, R> extends Production<NT, T> {

    public AttrProduction(NT s, Seq<? extends Either<NT, T>> r) {
        super(s, r);
    }

    // An abstract syntax tree will simply be a function which takes an environment Dynamic and returns
    // some value dynamic.
    // The environment represents the attributes passed down from the parent.
    // The Value returned the attributes passed up from the children.

    // This production will state the rules for how to build ASTs.
    protected abstract R buildResultUnsafe(Seq<? extends R> children);

    public R buildResult(Seq<? extends R> children) {
        Objects.requireNonNull(children);
        children.forEach(Objects::requireNonNull);

        if (children.length() != getRule().length()) {
            throw new IllegalArgumentException("This rule requires " + getRule().length() + " tokens.");
        }

        return buildResultUnsafe(children);
    }
}
