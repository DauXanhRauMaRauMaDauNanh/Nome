package org.apache.commons.lang3;

public class StringUtils {
    public static final String EMPTY = "";

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }

    public static boolean isBlank(CharSequence cs) {
        if (cs == null) return true;
        int len = cs.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    public static String join(Object[] array, String separator) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0 && separator != null) {
                sb.append(separator);
            }
            if (array[i] != null) {
                sb.append(array[i]);
            }
        }
        return sb.toString();
    }

    public static String join(Iterable<?> iterable, String separator) {
        if (iterable == null) return null;
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object o : iterable) {
            if (!first && separator != null) {
                sb.append(separator);
            }
            first = false;
            if (o != null) {
                sb.append(o);
            }
        }
        return sb.toString();
    }
}
