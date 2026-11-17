package io.github.doguomer.esign.pkcs11;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Pkcs11LibraryLocatorTest {

    @Test
    void envVariableWins() {
        var loc = new Pkcs11LibraryLocator(Map.of(Pkcs11LibraryLocator.ENV_VAR, "/opt/x.so")::get,
                p -> true, "Linux");
        assertEquals(Path.of("/opt/x.so"), loc.locate().orElseThrow());
    }

    @Test
    void missingEnvTargetGivesEmptyInsteadOfFallingBack() {
        var loc = new Pkcs11LibraryLocator(Map.of(Pkcs11LibraryLocator.ENV_VAR, "/opt/yok.so")::get,
                p -> p.toString().equals("/usr/lib/libakisp11.so"), "Linux");
        assertTrue(loc.locate().isEmpty());
    }

    @Test
    void linuxFallbackFindsFirstExisting() {
        Set<String> present = Set.of("/usr/local/lib/libakisp11.so");
        var loc = new Pkcs11LibraryLocator(k -> null, p -> present.contains(p.toString()), "Linux");
        assertEquals(Path.of("/usr/local/lib/libakisp11.so"), loc.locate().orElseThrow());
    }

    @Test
    void macUsesDylibCandidates() {
        var loc = new Pkcs11LibraryLocator(k -> null, p -> false, "Mac OS X");
        assertTrue(loc.candidates().stream().allMatch(c -> c.endsWith(".dylib")));
    }

    @Test
    void unknownOsHasNoCandidates() {
        var loc = new Pkcs11LibraryLocator(k -> null, p -> true, "FreeBSD");
        assertTrue(loc.locate().isEmpty());
    }

    @Test
    void windowsFindsAkisDllInSystem32() {
        var env = Map.of("SystemRoot", "D:\\Win", "ProgramFiles", "C:\\Program Files");
        String expected = "D:\\Win\\System32\\akisp11.dll";
        var loc = new Pkcs11LibraryLocator(env::get, p -> p.toString().equals(expected), "Windows 11");
        assertEquals(expected, loc.locate().orElseThrow().toString());
        assertTrue(loc.candidates().contains("C:\\Program Files\\AKIS\\akisp11.dll"));
    }

    @Test
    void windowsDefaultsWithoutEnvironment() {
        var loc = new Pkcs11LibraryLocator(k -> null, p -> false, "Windows Server 2025");
        assertEquals("C:\\Windows\\System32\\akisp11.dll", loc.candidates().get(0));
        assertTrue(loc.locate().isEmpty());
    }

    @Test
    void windowsEnvOverrideStillWins() {
        var loc = new Pkcs11LibraryLocator(Map.of(Pkcs11LibraryLocator.ENV_VAR, "E:\\kart\\p11.dll")::get,
                p -> true, "Windows 11");
        assertEquals("E:\\kart\\p11.dll", loc.locate().orElseThrow().toString());
    }
}
