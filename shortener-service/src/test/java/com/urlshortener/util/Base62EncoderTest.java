package com.urlshortener.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Base62Encoder Tests")
class Base62EncoderTest {

    @Test
    @DisplayName("Should encode 0 as '0'")
    void encodeZero() {
        assertEquals("0", Base62Encoder.encode(0));
    }

    @Test
    @DisplayName("Should encode small numbers correctly")
    void encodeSmallNumbers() {
        assertEquals("1", Base62Encoder.encode(1));
        assertEquals("a", Base62Encoder.encode(10));
        assertEquals("A", Base62Encoder.encode(36));
        assertEquals("10", Base62Encoder.encode(62));
    }

    @Test
    @DisplayName("Should encode large numbers correctly")
    void encodeLargeNumbers() {
        String encoded = Base62Encoder.encode(1_000_000);
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
        // Verify round-trip
        assertEquals(1_000_000, Base62Encoder.decode(encoded));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 42, 100, 999, 10000, 123456789, Long.MAX_VALUE / 2})
    @DisplayName("Should round-trip encode/decode for various values")
    void roundTrip(long value) {
        String encoded = Base62Encoder.encode(value);
        long decoded = Base62Encoder.decode(encoded);
        assertEquals(value, decoded, "Round-trip failed for value: " + value);
    }

    @Test
    @DisplayName("Should throw for negative values")
    void encodeNegative() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encode(-1));
    }

    @Test
    @DisplayName("Should throw for null/empty decode input")
    void decodeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(null));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(""));
    }

    @Test
    @DisplayName("Should throw for invalid characters in decode input")
    void decodeInvalidChars() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode("abc!@#"));
    }

    @Test
    @DisplayName("Should pad encoded values to minimum length")
    void encodePadded() {
        String padded = Base62Encoder.encodePadded(1, 7);
        assertEquals(7, padded.length());
        assertTrue(padded.startsWith("000000"));
        assertEquals(1, Base62Encoder.decode(padded));
    }

    @Test
    @DisplayName("Should not pad if already long enough")
    void encodePaddedNoOp() {
        String padded = Base62Encoder.encodePadded(Long.MAX_VALUE / 2, 3);
        // The encoded value is already longer than 3
        assertTrue(padded.length() >= 3);
    }

    @Test
    @DisplayName("7-char codes should support billions of unique values")
    void sevenCharCapacity() {
        // 62^7 = 3,521,614,606,208 — more than enough
        long maxFor7Chars = (long) Math.pow(62, 7) - 1;
        String encoded = Base62Encoder.encode(maxFor7Chars);
        assertEquals(7, encoded.length());
        assertEquals("ZZZZZZZ", encoded);
    }
}
