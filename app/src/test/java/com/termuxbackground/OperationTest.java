package com.termuxbackground;
import org.junit.Test;
import static org.junit.Assert.*;

public class OperationTest {
    @Test public void applyArgumentsAreLiteral() {
        assertArrayEquals(new String[]{"set","--image-stdin","--opacity","0.65","--blur","true","--animation","scroll"}, Operation.arguments("APPLY","0.65",true,"scroll"));
    }
    @Test public void operationsAreAllowlisted() {
        for (String op : new String[]{null,"sh","apply","","STATUS;id","/bin/bash"}) {
            try { Operation.arguments(op,null,false,null); fail(op); } catch (IllegalArgumentException expected) { }
        }
        assertArrayEquals(new String[]{"reset"},Operation.arguments("RESET",null,false,null));
        assertArrayEquals(new String[]{"probe"},Operation.arguments("PROBE",null,false,null));
    }
    @Test public void rejectsInvalidNumbersAndInjection() {
        for (String value : new String[]{"NaN","Infinity","-1","1.1","0;id","$(id)"," 0.5","0\n","1e-2","0.1234",""}) {
            try { Operation.arguments("APPLY",value,false,"none"); fail(value); } catch (IllegalArgumentException expected) { }
        }
        for (String value : new String[]{"0","1","0.001","1.000"}) Operation.arguments("APPLY",value,false,"none");
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsAnimationInjection() { Operation.arguments("APPLY","0.5",false,"none;id"); }
    @Test public void imageBoundsAndTypes() {
        Operation.validateImage(100,100,"image/png");
        for (int[] size : new int[][]{{0,100},{-1,10},{100000,100000}}) {
            try { Operation.validateImage(size[0],size[1],"image/jpeg"); fail(); } catch (IllegalArgumentException expected) { }
        }
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsUnsupportedImage() { Operation.validateImage(10,10,"image/svg+xml"); }
    @Test public void resultsAndTimeout() {
        assertEquals("COMPLETED",Operation.resultCode(-1,0));
        assertEquals("EXECUTION_ERROR",Operation.resultCode(1,0));
        assertEquals("CONFIG_WRITE_FAILED",Operation.resultCode(-1,5));
        assertEquals("RELOAD_FAILED",Operation.resultCode(-1,6));
        assertEquals("CLI_MISSING",Operation.resultCode(-1,127));
        assertEquals("CLI_MISSING",Operation.resultCode(150,-999));
        assertEquals("CLI_VALIDATION_FAILED",Operation.resultCode(-1,2));
        assertEquals("RELOAD_UNAVAILABLE",Operation.resultCode(-1,3));
        assertEquals("IMAGE_TRANSFER_FAILED",Operation.resultCode(-1,4));
        assertEquals("CLEANUP_FAILED",Operation.resultCode(-1,7));
        assertEquals("DEPENDENCY_MISSING",Operation.resultCode(-1,8));
        assertEquals("UNEXPECTED_FAILURE",Operation.resultCode(-1,42));
        assertFalse(Operation.timedOut(59999)); assertTrue(Operation.timedOut(60000));
    }
    @Test public void monetBackendIsFixedAndCapabilitiesEnforced() {
        assertArrayEquals(new String[]{"--backend","monet","reset"}, Operation.monetArguments("RESET",null,false,null));
        assertArrayEquals(new String[]{"--backend","monet","set","--image-stdin","--opacity","0.5","--blur","false","--animation","none"}, Operation.monetArguments("APPLY","0.5",false,"none"));
        for (Object[] values : new Object[][]{{true,"none"},{false,"scroll"}}) {
            try { Operation.monetArguments("APPLY","0.5",(Boolean)values[0],(String)values[1]); fail(); } catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void frontendTypesAreStrict() {
        for (Object[] values : new Object[][]{{0.5, false, "none"}, {"0.5", "false", "none"}, {"0.5", false, null}}) {
            try { Operation.validateValues(values[0], values[1], values[2]); fail(); } catch (IllegalArgumentException expected) { }
        }
        Operation.validateValues("0.5", false, "none");
    }
    @Test public void protocolParsing() {
        assertTrue(Operation.protocolSupported("TBG_PROTOCOL=1\nCONFIG=WRITABLE\n"));
        assertFalse(Operation.protocolSupported("TBG_PROTOCOL=10\n"));
        assertFalse(Operation.protocolSupported(null));
    }
}
