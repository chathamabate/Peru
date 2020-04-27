package org.perudevteam.lexer.charlexer;

import io.vavr.Function1;
import org.perudevteam.fa.DFAutomaton;
import org.perudevteam.lexer.SimpleDLexer;
import org.perudevteam.misc.LineException;

/**
 * A lexer for lexing characters into strings using the simple lexing algorithm.
 * This lexer will categorize a successful lexeme with some <b>Enum</b>.
 * <br>
 * For example, categorizing some string matching the regex <b>[0-9]+</b> as an <i>INTEGER</i>.
 *
 * @param <T> The category type.
 */
public class CharSimpleDLexer<T extends Enum<T>> extends
        SimpleDLexer<Character, String, CharData<T>, CharSimpleContext> {

    /**
     * Constructor.
     *
     * @param d The lexer's deterministic finite automaton.
     */
    public CharSimpleDLexer(DFAutomaton<? super Character, ?,
                    ? extends Function1<? super CharSimpleContext, ? extends CharData<T>>> d) {
        super("", d);
    }

    @Override
    protected CharSimpleContext readInput(Character input, CharSimpleContext context) {
        // New Line Character means current line increments and current line pos goes to one..
        // Otherwise line stays the same, line position increments.
        return input == '\n'
                ? context.map(l -> l.withCurrent(l.getCurrent() + 1), lp -> lp.withCurrent(1))
                : context.mapLinePosition(lp -> lp.withCurrent(lp.getCurrent() + 1));
    }

    @Override
    protected String combineInput(String lexeme, Character input) {
        return lexeme + input;
    }

    @Override
    protected CharSimpleContext onToken(String lexeme, CharData<T> data, CharSimpleContext context) {
        // Here current line becomes ending line, and current position becomes ending position.
        return context.map(l -> l.withEnding(l.getCurrent()), lp -> lp.withEnding(lp.getCurrent()));
    }

    @Override
    protected LineException makeError(String lexeme, CharSimpleContext context) {
        return LineException.lineEx(context.getLine().getStarting(), context.getLinePosition().getStarting(),
                "Lexeme cannot be lexed.");
    }

    @Override
    protected CharSimpleContext onError(String lexeme, CharSimpleContext context) {
        return context.map(l -> l.withEnding(l.getCurrent()).withStarting(l.getCurrent()),
                lp -> lp.withEnding(lp.getCurrent()).withStarting(lp.getCurrent()));
    }

    @Override
    protected CharSimpleContext onSuccess(String lexeme, CharData<T> data, CharSimpleContext context) {
        // Here we restart the context to whatever comes directly after the successful token.
        return context.map(l -> l.withCurrent(l.getEnding()).withStarting(l.getEnding()),
                lp -> lp.withCurrent(lp.getEnding()).withStarting(lp.getEnding()));
    }
}
