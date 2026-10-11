package org.slf4j.helpers;

public final class MessageFormatter {

    public static FormattingTuple format(String messagePattern, Object arg) {
        return arrayFormat(messagePattern, new Object[]{arg});
    }

    public static FormattingTuple format(String messagePattern, Object arg1, Object arg2) {
        return arrayFormat(messagePattern, new Object[]{arg1, arg2});
    }

    public static FormattingTuple arrayFormat(String messagePattern, Object[] argArray) {
        if (messagePattern == null) {
            return new FormattingTuple(null, argArray, null);
        }
        if (argArray == null || argArray.length == 0) {
            return new FormattingTuple(messagePattern, argArray, null);
        }

        Throwable throwableCandidate = getThrowableCandidate(argArray);
        Object[] argsToUse = argArray;
        if (throwableCandidate != null) {
            argsToUse = trimmedCopy(argArray);
        }

        StringBuilder sbuf = new StringBuilder(messagePattern.length() + 50);
        int i = 0;
        int L = 0;
        for (L = 0; L < argsToUse.length; L++) {
            int j = messagePattern.indexOf("{}", i);
            if (j == -1) {
                if (i == 0) {
                    return new FormattingTuple(messagePattern, argsToUse, throwableCandidate);
                }
                sbuf.append(messagePattern, i, messagePattern.length());
                return new FormattingTuple(sbuf.toString(), argsToUse, throwableCandidate);
            }
            sbuf.append(messagePattern, i, j);
            sbuf.append(argsToUse[L]);
            i = j + 2;
        }
        sbuf.append(messagePattern, i, messagePattern.length());
        return new FormattingTuple(sbuf.toString(), argsToUse, throwableCandidate);
    }

    static Throwable getThrowableCandidate(Object[] argArray) {
        if (argArray == null || argArray.length == 0) return null;
        Object last = argArray[argArray.length - 1];
        if (last instanceof Throwable) {
            return (Throwable) last;
        }
        return null;
    }

    static Object[] trimmedCopy(Object[] argArray) {
        if (argArray == null || argArray.length == 0) return argArray;
        Object[] trimmed = new Object[argArray.length - 1];
        System.arraycopy(argArray, 0, trimmed, 0, trimmed.length);
        return trimmed;
    }
}
