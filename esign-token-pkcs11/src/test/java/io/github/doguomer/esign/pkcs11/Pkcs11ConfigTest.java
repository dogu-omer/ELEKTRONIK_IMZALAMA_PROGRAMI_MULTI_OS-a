package io.github.doguomer.esign.pkcs11;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Pkcs11ConfigTest {

    @Test
    void rendersInlineConfig() {
        String cfg = new Pkcs11Config(Path.of("/usr/lib/libakisp11.so"), 1).toSunPkcs11Config();
        assertTrue(cfg.startsWith("--name=esign-slot1\n"));
        assertTrue(cfg.contains("library=\"/usr/lib/libakisp11.so\""));
        assertTrue(cfg.contains("slotListIndex=1"));
    }

    @Test
    void rejectsNewlineInjection() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pkcs11Config(Path.of("/tmp/a.so\nattributes=compatibility"), 0));
    }

    @Test
    void rejectsNegativeSlot() {
        assertThrows(IllegalArgumentException.class, () -> new Pkcs11Config(Path.of("/a.so"), -1));
    }
}
