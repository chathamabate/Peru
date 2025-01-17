package com.github.chathamabate.peru.parser;

import com.github.chathamabate.peru.parser.grammar.CFGrammar;
import com.github.chathamabate.peru.parser.grammar.Production;
import com.github.chathamabate.peru.parser.grammar.SemanticCFGrammar;
import com.github.chathamabate.peru.parser.grammar.SemanticProduction;
import com.github.chathamabate.peru.parser.lrone.FirstSets;
import io.vavr.CheckedFunction2;
import io.vavr.Tuple2;
import io.vavr.collection.*;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.*;

public class TestCFGrammar {

    enum NT {
        A, B, C, D
    }

    enum T {
        E, F, G, H
    }

    /*
     * Production.
     */

    @Test
    void testProductionErrors() {
        assertThrows(NullPointerException.class, () -> new Production<>(NT.A, null));
        assertThrows(NullPointerException.class, () -> new Production<>(NT.A, List.of(null, right(T.F))));
        assertThrows(NullPointerException.class, () -> new Production<NT, T>(NT.A, List.of(left(null))));
        assertThrows(NullPointerException.class, () -> new Production<NT, T>(NT.A, List.of(right(null))));
    }

    @Test
    void testWithRule() {
        Production<NT, T> prod = new Production<>(NT.A, List.of(
           right(T.E), left(NT.A), left(NT.B)
        ));

        // Terminal Symbol switch.
        Production<NT, NT> prod2 = prod.withRule(List.of(
           right(NT.A)
        ));

        Production<NT, T> prod3 = prod.withSource(NT.B);

        assertNotEquals(prod, prod2);
        assertNotEquals(prod, prod3);
        assertNotEquals(prod2, prod3);

        assertThrows(NullPointerException.class, () -> prod.withRule(null));
        assertThrows(NullPointerException.class, () -> prod.withSource(null));
    }

    /*
     * Simple Grammar Tests.
     */

    @Test
    void testGrammarBasics() {
        Production<NT, T> prod1 = new Production<>(NT.A, List.of(
                left(NT.B), right(T.E)
        ));

        Production<NT, T> prod2 = new Production<>(NT.B, List.of(
                right(T.F)
        ));

        Production<NT, T> prod3 = prod1.withSource(NT.C).withRule(List.of(
                left(NT.B), right(T.F), left(NT.C), left(NT.A)
        ));

        assertThrows(IllegalArgumentException.class, () -> new CFGrammar<>(NT.C, List.of(prod1, prod2)));
        assertThrows(IllegalArgumentException.class, () -> new CFGrammar<>(NT.C, List.of(prod3, prod2)));

        Production<NT, T> prod4 = new Production<>(NT.A, List.of(
                left(NT.B), right(T.E)
        ));

        Production<NT, T> prod5 = new Production<>(NT.B, List.of(
                left(NT.A), right(T.E)
        ));

        assertThrows(IllegalArgumentException.class, () -> new CFGrammar<>(NT.A, List.of(prod4)));

        CFGrammar<NT, T, Production<NT, T>> g = new CFGrammar<>(NT.A, List.of(prod4, prod5));
        assertEquals(HashSet.of(NT.B, NT.A), g.getNonTerminalsUsed());
        assertEquals(HashSet.of(T.E), g.getTerminalsUsed());
    }

    /*
     * Firsts Sets Tests.
     */

    private static final CFGrammar<NT, T, Production<NT, T>> G1 = new CFGrammar<>(NT.A, List.of(
            new Production<>(NT.A, List.of(right(T.E)))
    ));

    private static final FirstSets<NT, T> F1 = new FirstSets<>(G1);

    private static final Production<NT, T>
            PROD1 = new Production<NT, T>(NT.A, List.of(left(NT.B), left(NT.C))),       // A -> BC
            PROD2 = new Production<>(NT.B, List.of(right(T.E))),                        // B -> e
            PROD3 = new Production<>(NT.C, List.of(right(T.F))),                        // C -> f
            PROD4 = new Production<>(NT.B, List.empty()),                               // B ->
            PROD5 = new Production<>(NT.C, List.empty());                               // C ->


    private static final CFGrammar<NT, T, Production<NT, T>> G2 = new CFGrammar<>(NT.A, List.of(
            PROD1, PROD2, PROD3
    ));

    private static final FirstSets<NT, T> F2 = new FirstSets<>(G2);

    private static final CFGrammar<NT, T, Production<NT, T>> G3 = G2.withProduction(
            PROD4
    );

    private static final FirstSets<NT, T> F3 = new FirstSets<>(G3);

    private static final CFGrammar<NT, T, Production<NT, T>> G4 = G3.withProduction(
            PROD5
    );

    private static final FirstSets<NT, T> F4 = new FirstSets<>(G4);

    @Test
    void testFirstsSets() {
        assertEquals(HashSet.of(T.E), F1.getFirstSet(NT.A)._2);

        assertEquals(HashSet.of(T.E), F2.getFirstSet(NT.A)._2);
        assertEquals(HashSet.of(T.F), F2.getFirstSet(NT.C)._2);
        assertFalse(F2.getFirstSet(NT.A)._1);

        assertEquals(HashSet.of(T.E, T.F), F3.getFirstSet(NT.A)._2);
        assertTrue(F3.getFirstSet(NT.B)._1);

        assertTrue(F4.getFirstSet(NT.A)._1 && F4.getFirstSet(NT.B)._1 && F4.getFirstSet(NT.C)._1);

        Tuple2<Boolean, Set<T>> ruleTuple = F4.getFirstSet(List.of(left(NT.A), right(T.G), right(T.H)));
        assertFalse(ruleTuple._1);
        assertEquals(HashSet.of(T.E, T.F, T.G), ruleTuple._2);
    }

    /*
     * Simple AttrCFGrammar Error Tests.
     */

    private static final Seq<Either<NT, T>> A_RULE1 = List.of(
        right(T.E)
    );

    private static final SemanticProduction<NT, T, String> A_PROD1 =
            new SemanticProduction<NT, T, String>(NT.A, A_RULE1) {
        @Override
        protected String buildResultUnchecked(Seq<String> children) {
            return "";
        }
    };

    private static final Map<T, CheckedFunction2<String, Tokenized<T>, String>> TERM_RES_GENS1 = HashMap.of(
        T.E, (l, d) -> l
    );

    private static final SemanticCFGrammar<NT, T, SemanticProduction<NT, T, String>, String, Tokenized<T>, String> A_G1 =
            new SemanticCFGrammar<>(NT.A, TERM_RES_GENS1, List.of(A_PROD1));

    private static final Seq<Either<NT, T>> A_RULE2 = List.of(right(T.H));
    private static final SemanticProduction<NT, T, String> A_PROD2 = new SemanticProduction<NT, T, String>(NT.A, A_RULE2) {
        @Override
        protected String buildResultUnchecked(Seq<String> children) {
            return "";
        }
    };

    @Test
    void testSemanticGrammarErrors() {
        // Test adding a production with unseen terminals throws an error.
        assertThrows(IllegalArgumentException.class, () -> A_G1.withProduction(A_PROD2));
    }
}












