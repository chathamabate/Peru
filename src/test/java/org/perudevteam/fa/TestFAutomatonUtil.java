package org.perudevteam.fa;

import io.vavr.Function1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.*;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.perudevteam.lexer.charlexer.CharData;
import org.perudevteam.lexer.charlexer.CharSimpleContext;
import org.perudevteam.lexer.charlexer.CharSimpleDLexer;

import static org.perudevteam.fa.FAutomatonUtil.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestFAutomatonUtil {

    private static final Array<Set<Integer>> TEST_GRAPH = Array.of(
            HashSet.of(1),
            HashSet.of(2),
            HashSet.of(1),
            HashSet.empty(),
            HashSet.of(2, 3)
    );

    private static final Array<Set<Integer>> EXPECTED_REACHABLES = Array.of(
            HashSet.of(0, 1, 2),
            HashSet.of(1, 2),
            HashSet.of(1, 2),
            HashSet.of(3),
            HashSet.of(1, 2, 3, 4)
    );

    @Test
    void testReachableSets() {
        assertEquals(EXPECTED_REACHABLES, reachableSets(TEST_GRAPH));
    }

    /*
     * NFA to DFA tests.
     *
     * Must test by lexing some input.
     */

    enum InputClass {
        A,
        B,
        C
    }

    private static InputClass getInputClass(Character c) {
        if (c == 'a') return InputClass.A;
        if (c == 'b') return InputClass.B;
        if (c == 'c') return InputClass.C;

        throw new IllegalArgumentException("Bad Input Value Given.");
    }

    enum OutputClass {
        THING1,
        THING2
    }

    private static final Function1<CharSimpleContext, CharData<OutputClass>>
        OUTPUT_1 = c -> new CharData<>(OutputClass.THING1, c),
        OUTPUT_2 = c -> new CharData<>(OutputClass.THING2, c);

    private static final NFAutomaton<Character, InputClass, OutputClass> AMBIGUOUS_NFA =
            new NFAutomaton<Character, InputClass, OutputClass>(
            5, HashSet.of(InputClass.values()), TestFAutomatonUtil::getInputClass
    )
                    .withSingleTransition(0,1, InputClass.A)
                    .withEpsilonTransition(1, 2)
                    .withAcceptingState(2, OutputClass.THING1)
                    .withEpsilonTransition(0, 3)
                    .withSingleTransition(3, 4, InputClass.A)
                    .withAcceptingState(4, OutputClass.THING2);

    @Test
    void testAmbiguousNFA() {
        assertThrows(IllegalArgumentException.class, AMBIGUOUS_NFA::toDFA);
    }

    private static final NFAutomaton<Character, InputClass, Function1<CharSimpleContext, CharData<OutputClass>>> NFA1 =
            new NFAutomaton<Character, InputClass, Function1<CharSimpleContext, CharData<OutputClass>>>(
                    9, HashSet.of(InputClass.values()), TestFAutomatonUtil::getInputClass
    )
                    .withEpsilonTransition(0, 1)
                    .withSingleTransition(1, 2, InputClass.A)
                    .withEpsilonTransition(2, 3)
                    .withSingleTransition(3, 4, InputClass.B)
                    .withAcceptingState(4, OUTPUT_1)

                    .withSingleTransition(0, 5, InputClass.A)
                    .withEpsilonTransition(5, 6)
                    .withEpsilonTransition(5, 8)
                    .withSingleTransition(6, 7, InputClass.C)
                    .withEpsilonTransition(7, 8)
                    .withEpsilonTransition(8, 6)
                    .withAcceptingState(8, OUTPUT_2);

    private static final DFAutomaton<Character, InputClass, Function1<CharSimpleContext, CharData<OutputClass>>>
            DFA1 = NFA1.toDFA();

    private static final CharSimpleDLexer<OutputClass> LEXER1 = new CharSimpleDLexer<>(DFA1);

    private static final Seq<Tuple2<String, OutputClass>> EXPECTED1 = List.of(
            Tuple.of("ab", OutputClass.THING1),
            Tuple.of("a", OutputClass.THING2),
            Tuple.of("ac", OutputClass.THING2),
            Tuple.of("acc", OutputClass.THING2),
            Tuple.of("accccc", OutputClass.THING2)
    );

    @TestFactory
    Seq<DynamicTest> testNFAtoDFALexer() {
        return EXPECTED1.map(tuple -> DynamicTest.dynamicTest("String : " + tuple._1, () -> {
            Seq<Character> inputSequence = List.ofAll(tuple._1.toCharArray());

            Seq<Tuple2<String, CharData<OutputClass>>> outputs =
                    LEXER1.buildOnlySuccessfulTokenStream(inputSequence, CharSimpleContext.INIT_SIMPLE_CONTEXT);

            assertEquals(1, outputs.length());
            Tuple2<String, CharData<OutputClass>> result = outputs.head();

            assertEquals(tuple._1, result._1);
            assertEquals(tuple._2, result._2.getTokenType());
        }));
    }

    private static final Seq<String> FAILURES1 = List.of(
            "ba",
            "caa",
            "ccc",
            "bbbb"
    );

    @TestFactory
    Seq<DynamicTest> testExpectedErrors() {
        return FAILURES1.map(failure -> DynamicTest.dynamicTest("Failure : " + failure, () ->
            assertTrue(LEXER1.build(List.ofAll(failure.toCharArray()), CharSimpleContext.INIT_SIMPLE_CONTEXT)
                    ._1._2.isFailure())));
    }

    /*
     * Precedence Tests.
     */

    private static final NFAutomaton<Character, InputClass, OutputClass>
            NFA2 = new NFAutomaton<Character, InputClass, OutputClass>(
            3, HashSet.of(InputClass.values()), TestFAutomatonUtil::getInputClass
    )
            .withSingleTransition(0, 1, InputClass.A)
            .withAcceptingState(1, OutputClass.THING1)

            .withSingleTransition(0, 2, InputClass.A)
            .withAcceptingState(2, OutputClass.THING1);

    @Test
    void checkSimplePrecedence() {
        NFA2.toDFA();

        // Precedence conflict.
        assertThrows(IllegalArgumentException.class,
                () -> NFA2.withAcceptingState(2, OutputClass.THING2).toDFA());

        NFA2.withAcceptingState(2, OutputClass.THING2).toDFA(
                List.of(HashSet.of(OutputClass.THING1), HashSet.of(OutputClass.THING2))
        );
    }

}
