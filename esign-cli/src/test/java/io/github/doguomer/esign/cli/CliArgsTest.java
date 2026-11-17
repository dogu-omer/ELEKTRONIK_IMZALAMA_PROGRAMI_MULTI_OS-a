package io.github.doguomer.esign.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CliArgsTest {

    @Test
    void parsesListCerts() {
        var a = CliArgs.parse(List.of("list-certs", "--lib", "/x.so", "--slot", "2", "--login"));
        assertEquals("list-certs", a.command());
        assertEquals(Path.of("/x.so"), a.library().orElseThrow());
        assertEquals(2, a.slot());
        assertTrue(a.login());
    }

    @Test
    void emptyMeansHelp() {
        assertEquals("help", CliArgs.parse(List.of()).command());
    }

    @Test
    void pinArgumentIsRejected() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> CliArgs.parse(List.of("list-certs", "--pin", "1234")));
        assertFalse(e.getMessage().contains("1234"));
    }

    @Test
    void badSlotRejected() {
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(List.of("list-certs", "--slot", "-1")));
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(List.of("list-certs", "--slot")));
    }
}
