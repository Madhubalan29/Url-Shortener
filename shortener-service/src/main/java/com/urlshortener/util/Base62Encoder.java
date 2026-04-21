package com.urlshortener.util;

/**
 * Base62 encoder for generating short URL codes.
 *
 * Encodes numeric IDs to Base62 strings using characters [0-9a-zA-Z].
 * A 7-character code supports 62^7 ≈ 3.5 trillion unique URLs.
 */
public final class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length(); // 62

    private Base62Encoder() {
        // Utility class — no instantiation
    }

    /**
     * Encode a positive long value to a Base62 string.
     *
     * @param value the numeric value to encode (must be >= 0)
     * @return Base62 encoded string
     * @throws IllegalArgumentException if value is negative
     */
    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative: " + value);
        }
        if (value == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decode a Base62 string back to a long value.
     *
     * @param encoded the Base62 string to decode
     * @return the decoded numeric value
     * @throws IllegalArgumentException if the string contains invalid characters
     */
    public static long decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Encoded string must not be null or empty");
        }

        long value = 0;
        for (char c : encoded.toCharArray()) {
            int index = ALPHABET.indexOf(c);
            if (index < 0) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            value = value * BASE + index;
        }
        return value;
    }

    /**
     * Encode a value and pad the result to a minimum length.
     *
     * @param value     the numeric value to encode
     * @param minLength minimum length of the output string
     * @return padded Base62 encoded string
     */
    public static String encodePadded(long value, int minLength) {
        String encoded = encode(value);
        while (encoded.length() < minLength) {
            encoded = ALPHABET.charAt(0) + encoded;
        }
        return encoded;
    }
}
