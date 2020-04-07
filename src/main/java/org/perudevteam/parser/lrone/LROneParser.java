package org.perudevteam.parser.lrone;

import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.perudevteam.misc.Typed;
import org.perudevteam.parser.Parser;
import org.perudevteam.parser.grammar.AttrCFGrammar;
import org.perudevteam.parser.grammar.AttrProduction;

import java.util.Map;
import java.util.Objects;

/**
 * LR(1) Parser Class.
 *
 * @param <NT> Non Terminal enum type.
 * @param <T> Terminal Enum Type.
 * @param <L> Lexeme Type.
 * @param <D> Data Type.
 * @param <R> Result type produced by this parser.
 */
public abstract class LROneParser<NT extends Enum<NT>, T extends Enum<T>, L, D extends Typed<T>, R>
        implements Parser<T, L, D, R> {

    private AttrCFGrammar<NT, T, AttrProduction<NT, T, R>, L, D, R> g;
    private LROneTable<NT, T, AttrProduction<NT, T, R>> table;

    @SuppressWarnings("unchecked")
    public LROneParser(AttrCFGrammar<NT, T, ? extends AttrProduction<NT, T, R>, L, D, R> grammar) {
        Objects.requireNonNull(grammar);
        g = (AttrCFGrammar<NT, T, AttrProduction<NT, T, R>, L, D, R>) grammar;
        table = new LROneTable<>(g);    // Build LR(1) table.
    }

    /**
     * Creates error to be thrown when an error state is encountered with the given lookahead.
     */
    protected abstract Throwable onError(Tuple2<L, D> lookAhead);

    /**
     * Creates error to be thrown when an error state is encountered with a lookahead of EOF.
     */
    protected abstract Throwable onError();

    @Override
    public R parseUnchecked(Seq<Tuple2<L, D>> tokens) throws Throwable {
        List<Integer> stateStack = List.of(0);
        List<R> resultStack = List.empty();
        NT goal = g.getStartSymbol();

        Seq<Tuple2<L, D>> tokensLeft = tokens;

        while (true) {
            // We only use goto table when we reduce.
            // otherwise we use action table to end the parse or to get the next

            // Get current state.
            int state = stateStack.peek();
            Option<Tuple2<L, D>> lookaheadOpt = tokensLeft.headOption();
            Either<Integer, AttrProduction<NT, T, R>> move =
                    lookaheadOpt.map(lkh -> table.actionMove(state, lkh._2.getType()))
                            .getOrElse(table.actionMove(state));

            // Perform initial error check.
            if (move.isLeft() && move.getLeft() == 0) {
                throw lookaheadOpt.isEmpty() ? onError() : onError(lookaheadOpt.get());
            }

            if (move.isRight()) {
                // Reduction or Accept.
                AttrProduction<NT, T, R> production = move.get();
                int childrenSize = production.getRule().size();

                Seq<R> childResults = List.empty();

                for (int i = 0; i < childrenSize; i++) {
                    childResults = childResults.prepend(resultStack.head());
                    resultStack = resultStack.tail();   // Pop child results.
                    stateStack = stateStack.tail();     // Pop old states.
                }

                R result = production.buildResult(childResults);
                NT resultType = production.getSource();

                // Check for acceptance state.
                if (resultType.equals(goal) && lookaheadOpt.isEmpty()) {
                    return result;  // ACCEPT State!
                }

                resultStack = resultStack.prepend(result);  // Push result onto result stack.
                int gotoState = table.gotoShift(stateStack.peek(), resultType);

                if (gotoState == 0) {
                    throw lookaheadOpt.isEmpty() ? onError() : onError(lookaheadOpt.get());
                }

                // If not an error, simply push our new goto state onto the state stack.
                stateStack = stateStack.prepend(gotoState);
            } else {
                int shift = move.getLeft();

                // Since there was no error, we know this is a non-zero shift.
                // this can never occur with a lookahead of EOF.
                // There must be a lookahead here.
                Tuple2<L, D> lookahead = lookaheadOpt.get();

                // Performing the shift entails pushing lookahead result and current state
                // onto the state and result stacks.
                R terminalResult = g.buildTerminalResult(lookahead._1, lookahead._2);

                resultStack = resultStack.prepend(terminalResult);
                stateStack = stateStack.prepend(shift);

                // We must also move past the given token in the tokensLeft list.
                tokensLeft = tokensLeft.tail();
            }
        }
    }
}