package io.github.doguomer.esign.dss;

import static org.junit.jupiter.api.Assertions.*;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import java.net.URI;
import org.junit.jupiter.api.Test;

class TimestampingTest {

    @Test
    void noneProducesBes() {
        assertFalse(Timestamping.none().enabled());
        assertEquals("BES", Timestamping.none().profile());
        assertEquals(SignatureLevel.CAdES_BASELINE_B,
                Timestamping.none().level(SignatureLevel.CAdES_BASELINE_B, SignatureLevel.CAdES_BASELINE_T));
    }

    @Test
    void onlineProducesT() {
        Timestamping ts = Timestamping.online(URI.create("https://tsa.example.org/tsr"));
        assertTrue(ts.enabled());
        assertEquals("T", ts.profile());
        assertEquals("tsa.example.org", ts.description().orElseThrow());
    }

    @Test
    void rejectsNonHttpAndCredentialsInUrl() {
        assertThrows(IllegalArgumentException.class, () -> Timestamping.online(URI.create("ftp://tsa.example.org")));
        assertThrows(IllegalArgumentException.class,
                () -> Timestamping.online(URI.create("https://kullanici:sifre@tsa.example.org")));
    }
}
