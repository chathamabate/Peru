package com.github.chathamabate.peru.lexer;

import io.vavr.Function1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Try;
import com.github.chathamabate.peru.fa.DFAutomaton;

/**
 * This class represents a simple lexer. The algorithm used here simply traverses the given
 * automaton until failure. Then returns a token with the last successful data and lexeme to be
 * found.
 *
 * @param <I> The raw input type of the given automaton.
 * @param <L> The lexeme type of the lexer.
 * @param <D> The data type of the lexer.
 * @param <C> The context type of the lexer.
 */
public abstract class SimpleDLexer<I, L, D, C> extends DLexer<I, L, D, C> {

    /**
     * Lexer Constructor.
     *
     * @param initLex The initial lexeme to use.
     * @param d The deterministic finite automaton to be used by this lexer.
     */
    public SimpleDLexer(L initLex,
                        DFAutomaton<? super I, ?, ? extends Function1<? super C, ? extends D>> d) {
        super(initLex, d);
    }

    @Override
    public Tuple3<Tuple2<L, Try<D>>, C, Seq<I>> buildUnchecked(Seq<? extends I> input, C context) {
        C algoContext = context;
        Seq<I> tail = Seq.narrow(input);

        // Initial State and Lexeme.
        L lexeme = getInitialLexeme();
        Option<Integer> stateOp = Option.of(0);

        Tuple2<L, Try<D>> lastToken = null;
        Seq<I> lastTail = null;

        DFAutomaton<I, ?, Function1<C, D>> dfa = getDFA();

        while(!stateOp.isEmpty()) {
            int state = stateOp.get();

            // If we are on an accepting state.
            if (dfa.isAccepting(state)) {
                Function1<C, D> dataBuilder = dfa.getOutput(state);
                // Build data for token.
                D data = dataBuilder.apply(algoContext);

                lastToken = Tuple.of(lexeme,Try.success(data));
                lastTail = tail;    // Save tail position.

                // Signal Context.
                algoContext = onToken(lexeme, data, algoContext);
            }

            // Now to read next input...
            if (tail.isEmpty()) {
                break; // Out of inputs to read.
            }

            I next = tail.head();

            algoContext = readInput(next, algoContext);
            lexeme = combineInput(lexeme, next);

            tail = tail.tail(); // Advance through input.

            // Calc next State.
            stateOp = dfa.getTransitionAsOption(state, next);
        }

        if (lastToken == null) {
            Try<D> errorData = Try.failure(makeError(lexeme, algoContext));
            return Tuple.of(Tuple.of(lexeme, errorData), onError(lexeme, algoContext), tail);
        }

        algoContext = onSuccess(lastToken._1, lastToken._2.get(), algoContext);
        return Tuple.of(lastToken, algoContext, lastTail);
    }
}
