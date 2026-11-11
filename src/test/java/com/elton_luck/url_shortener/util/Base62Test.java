package com.elton_luck.url_shortener.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Base62Test {

    @Test
    void encode_knownValues() {
        assertEquals("0", Base62.encode(0));
        assertEquals("1", Base62.encode(1));
        assertEquals("Z", Base62.encode(61));
        assertEquals("10", Base62.encode(62));
    }
}
