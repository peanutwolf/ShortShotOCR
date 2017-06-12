package com.vigursky.numberocr;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.vigursky.numberocr", appContext.getPackageName());
    }

    @Test
    public void phoneNumberValid() throws Exception{
        assertFalse(Tools.isPhoneValidNumber("aaa"));
        assertFalse(Tools.isPhoneValidNumber(""));
        assertFalse(Tools.isPhoneValidNumber("1f51daa2"));
        assertFalse(Tools.isPhoneValidNumber("11"));
        assertFalse(Tools.isPhoneValidNumber("#118"));
        assertFalse(Tools.isPhoneValidNumber("1812)648-40-88"));
        assertFalse(Tools.isPhoneValidNumber("bbbad)648-40-88asd31"));
        assertFalse(Tools.isPhoneValidNumber("t+54 9 223wwwr123-4567"));

        assertTrue(Tools.isPhoneValidNumber("t812)648-40-88"));
        assertTrue(Tools.isPhoneValidNumber("aaa#89210988095daaf"));
        assertTrue(Tools.isPhoneValidNumber("118"));
        assertTrue(Tools.isPhoneValidNumber("tds(812)648-40-88"));
        assertTrue(Tools.isPhoneValidNumber("eeff <(650) 555-1212>"));
        assertTrue(Tools.isPhoneValidNumber("we1(408) 555-1289"));

        assertTrue(Tools.isPhoneValidNumber("4f2a#(11) (15) 1234-56784f2a#", "AR"));
        assertTrue(Tools.isPhoneValidNumber("4f2ae2(223) 15 123-4567aac", "AR"));
        assertTrue(Tools.isPhoneValidNumber("4f2a#(11) 15 1234-5678", "AR"));
        assertTrue(Tools.isPhoneValidNumber("t+54 9 223 123-4567", "AR"));
    }

    @Test
    public void getPhoneNumberMatches() throws Exception{
        assertEquals("89210988095",     Tools.findPhoneNumber("89210988095"));
        assertEquals("89210988095",     Tools.findPhoneNumber("asd#89210988095asdd"));
        assertEquals("8(821)123-12-11", Tools.findPhoneNumber("8(821)123-12-11"));
        assertEquals("8(821)123-12-11", Tools.findPhoneNumber("sdfvb8(821)123-12-11r2ff11"));
        assertEquals("812)648-40-88",   Tools.findPhoneNumber("t812)648-40-88"));
        assertEquals("118",             Tools.findPhoneNumber("#118"));
        assertEquals("(650) 555-1212",  Tools.findPhoneNumber("eeff <(650) 555-1212>"));
        assertEquals("(408) 555-1289",  Tools.findPhoneNumber("we1(408) 555-1289"));
        assertEquals("+18475797000",  Tools.findPhoneNumber("aw3+18475797000)"));

        assertEquals("(15) 1234-56784", Tools.findPhoneNumber("4f2a#(11) (15) 1234-56784f2a#", "AR"));
        assertEquals("(223) 15 123-4567", Tools.findPhoneNumber("4f2ae2(223) 15 123-4567aac", "AR"));
        assertEquals("(11) 15 1234-5678", Tools.findPhoneNumber("4f2a#(11) 15 1234-5678", "AR"));
        assertEquals("+54 9 223 123-4567", Tools.findPhoneNumber("t+54 9 223 123-4567", "AR"));

        assertNull(                     Tools.findPhoneNumber("asdasdd"));

    }
}
