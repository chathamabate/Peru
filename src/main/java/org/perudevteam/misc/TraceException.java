package org.perudevteam.misc;


import io.vavr.Function1;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Try;

import javax.sound.sampled.Line;
import java.util.Objects;

public class TraceException extends Exception implements Positioned {

    public static <T> Try<T> matchTraceEx(Try<? extends T> tryValue,
                                          Function1<? super T, ? extends T> valueMap,
                                          Function1<? super TraceException, ? extends TraceException> exMap) {
        return MiscHelpers.throwMatch(TraceException.class, tryValue, valueMap, exMap);
    }

    private static final TraceException EMPTY_TRACE = new TraceException(List.empty());

    public static TraceException emptyTrace() {
        return EMPTY_TRACE;
    }

    private final Seq<LineException> trace;

    private TraceException(Seq<? extends LineException> t) {
        super();
        Objects.requireNonNull(t);
        t.forEach(Objects::requireNonNull);

        trace = Seq.narrow(t);
    }

    private LineException headException() {
        if (trace.isEmpty()) {
            throw new NullPointerException("Exception has no trace.");
        }

        return trace.head();
    }

    @Override
    public String getMessage() {
        return headException().getMessage();
    }

    @Override
    public int getLine() {
        return headException().getLine();
    }

    @Override
    public int getLinePosition() {
        return headException().getLinePosition();
    }

    @Override
    public String toString() {
        if (trace.isEmpty()) {
            return "";
        }

        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(trace.head().toString());
        trace.tail().forEach(ex -> strBuilder.append("\n  ").append(ex));

        return strBuilder.toString();
    }

    public TraceException push(LineException e) {
        return new TraceException(trace.prepend(e));
    }
}