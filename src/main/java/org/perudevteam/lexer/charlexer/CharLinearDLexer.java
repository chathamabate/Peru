package org.perudevteam.lexer.charlexer;

import io.vavr.Function1;
import io.vavr.Tuple2;
import org.perudevteam.lexer.LinearDLexer;
import org.perudevteam.misc.LineException;
import org.perudevteam.statemachine.DStateMachine;

public abstract class CharLinearDLexer<CL, T extends Enum<T>>
        extends LinearDLexer<Character, CL, String, CharData<T>, CharLinearContext> {

    public static <CL, T extends Enum<T>> CharLinearDLexer<CL, T> charLinearDLexer(
            int mra,
            DStateMachine<? super CL, ? extends Function1<? super CharLinearContext, ? extends CharData<T>>> d,
            Function1<? super Character, ? extends CL> getClass
    ) {
        return new CharLinearDLexer<CL, T>(mra, d) {
            @Override
            protected CL inputClass(Character input) {
                return getClass.apply(input);
            }
        };
    }

    public static <CL, T extends Enum<T>> CharLinearDLexer<CL, T> charLinearDLexer(
            DStateMachine<? super CL, ? extends Function1<? super CharLinearContext, ? extends CharData<T>>> d,
            Function1<? super Character, ? extends CL> getClass
    ) {
        return new CharLinearDLexer<CL, T>(d) {
            @Override
            protected CL inputClass(Character input) {
                return getClass.apply(input);
            }
        };
    }

    protected CharLinearDLexer(int mra,
            DStateMachine<? super CL, ? extends Function1<? super CharLinearContext, ? extends CharData<T>>> d) {
        super(mra, "", d);
    }

    protected CharLinearDLexer(
            DStateMachine<? super CL, ? extends Function1<? super CharLinearContext, ? extends CharData<T>>> d) {
        super("", d);
    }

    @Override
    protected CharLinearContext readInput(Character input, CharLinearContext context) {
        // If we read a new line, the current line should be incremented, current line position should be set to 1.
        // Otherwise, just increment line position.
        return input == '\n'
                ? context.map(l -> l.withCurrent(l.getCurrent() + 1), lp -> lp.withCurrent(1))
                : context.mapLinePosition(lp -> lp.withCurrent(lp.getCurrent() + 1));
    }

    @Override
    protected String combineInput(String lexeme, Character input) {
        return lexeme + input;
    }

    @Override
    protected CharLinearContext onToken(String lexeme, CharData<T> data, CharLinearContext context) {
        // On token shift ending line and line position to current line and line position.
        return context.map(l -> l.withEnding(l.getCurrent()), lp -> lp.withEnding(lp.getCurrent()));
    }

    @Override
    protected Throwable makeError(String lexeme, CharLinearContext context) {
        return new LineException(context.getLine().getStarting(),
                context.getLinePosition().getStarting(), "Lexeme cannot be lexed." + lexeme);
    }

    @Override
    protected CharLinearContext onError(String lexeme, CharLinearContext context) {
        return context.map(l -> l.withStarting(l.getCurrent()).withEnding(l.getCurrent()),
                lp -> lp.withStarting(lp.getCurrent()).withEnding(lp.getCurrent()));
    }

    @Override
    protected CharLinearContext onSuccess(String lexeme, CharData<T> data, CharLinearContext context) {
        // Shift current back to ending, and starting up to ending.

        return context.map(l -> l.withStarting(l.getEnding()).withCurrent(l.getEnding()),
                lp -> lp.withStarting(lp.getEnding()).withCurrent(lp.getEnding()));
    }
}
