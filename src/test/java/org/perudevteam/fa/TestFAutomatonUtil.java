package org.perudevteam.fa;

import io.vavr.Function1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.*;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.perudevteam.lexer.DLexer;
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

    private static final NFAutomaton<Character, InputClass, OutputClass> AMBIGUOUS_NFA =
            new NFAutomaton<Character, InputClass, OutputClass>(
            5, HashSet.of(InputClass.values()), TestFAutomatonUtil::getInputClass
    )
                    .withSingleTransition(0,1, InputClass.A)
                    .withEpsilonTransition(1, 2)
                    .withAcceptingState(2, OutputClass.THING1)
                    .withEpsilonTransition(0, 3)
                    .withSingleTransition(3, 4, InputClass.A)
                    .withAcceptingState(4, OutputClass.THING1);

    @Test
    void testAmbiguousNFA() {
        assertThrows(IllegalArgumentException.class, () -> convertNFAToDFA(AMBIGUOUS_NFA));
    }

    private static final NFAutomaton<Character, InputClass, Function1<CharSimpleContext, CharData<OutputClass>>> NFA1 =
            new NFAutomaton<Character, InputClass, Function1<CharSimpleContext, CharData<OutputClass>>>(
                    9, HashSet.of(InputClass.values()), TestFAutomatonUtil::getInputClass
    )
                    .withEpsilonTransition(0, 1)
                    .withSingleTransition(1, 2, InputClass.A)
                    .withEpsilonTransition(2, 3)
                    .withSingleTransition(3, 4, InputClass.B)
                    .withAcceptingState(4, c -> new CharData<>(OutputClass.THING1, c))

                    .withEpsilonTransition(0, 5)
                    .withEpsilonTransition(5, 6)
                    .withEpsilonTransition(5, 8)
                    .withSingleTransition(6, 7, InputClass.C)
                    .withEpsilonTransition(7, 8)
                    .withEpsilonTransition(8, 6)
                    .withAcceptingState(8, c -> new CharData<>(OutputClass.THING2, c));

    private static final DFAutomaton<Character, InputClass, Function1<CharSimpleContext, CharData<OutputClass>>>
            DFA1 = convertNFAToDFA(NFA1);

    private static final CharSimpleDLexer<OutputClass> LEXER1 = new CharSimpleDLexer<>(DFA1);

    private static final Seq<Tuple2<String, OutputClass>> EXPECTED1 = List.of(
            Tuple.of("ab", OutputClass.THING1),
            Tuple.of("c", OutputClass.THING2),
            Tuple.of("cc", OutputClass.THING2),
            Tuple.of("ccccc", OutputClass.THING2)
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
}
