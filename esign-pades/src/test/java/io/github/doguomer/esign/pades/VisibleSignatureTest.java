package io.github.doguomer.esign.pades;

import static org.junit.jupiter.api.Assertions.*;

import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.Test;

class VisibleSignatureTest {

    @Test
    void parsesSpecs() {
        assertEquals(VisibleSignature.defaults(), VisibleSignature.parse("son"));
        assertEquals(VisibleSignature.defaults(), VisibleSignature.parse(""));
        assertEquals(2, VisibleSignature.parse("2").page());
        var v = VisibleSignature.parse("3, 50 ,700");
        assertEquals(3, v.page());
        assertEquals(50f, v.x());
        assertEquals(700f, v.y());
    }

    @Test
    void rejectsBadSpecs() {
        for (String bad : new String[] {"x", "1,2", "-1", "1,-5,3", "1,2,3,4"}) {
            assertThrows(IllegalArgumentException.class, () -> VisibleSignature.parse(bad), bad);
        }
    }

    @Test
    void commonNameKeepsTurkishCharacters() {
        assertEquals("ÖMER DOĞU", PadesBesSigner.commonName(new X500Principal("CN=ÖMER DOĞU, C=TR")));
    }
}
