package io.github.doguomer.esign.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class MainTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return Main.run(List.of(args), new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    @Test
    void helpExitsZero() {
        assertEquals(0, run("help"));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("list-certs"));
    }

    @Test
    void unknownCommandExitsTwo() {
        assertEquals(2, run("sil-her-seyi"));
    }

    @Test
    void missingLibraryExitsOne() {
        assertEquals(1, run("list-certs", "--lib", "/olmayan/libakisp11.so"));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Hata"));
    }
}
