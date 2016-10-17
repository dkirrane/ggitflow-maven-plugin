/*
 * Copyright 2016 dkirrane.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dkirrane.maven.plugins.ggitflow.name;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author dkirrane
 */
public class NamerImplTest {

    private Namer namer;

    public NamerImplTest() {
        namer = new NamerImpl();
    }

    @Test
    public void testTrimRefName() {
        String name = "/.sup\\p/.o..r//t/.lock";
//        String expResult = "support/";
//        String result = namer.trimRefName(name);
//        assertEquals(expResult, result);

        assertEquals("support", namer.trimRefName("support."));
        assertEquals("support", namer.trimRefName("support.lock"));
        assertEquals("support", namer.trimRefName("/.sup/.port/."));
        assertEquals("support", namer.trimRefName("..supp..ort.."));
        assertEquals("support", namer.trimRefName("supp ort"));
        assertEquals("support", namer.trimRefName("supp  ort"));
        assertEquals("support", namer.trimRefName("supp  ~^:ort"));
        assertEquals("support", namer.trimRefName("supp?*[]ort"));
        assertEquals("supp/ort", namer.trimRefName("/supp/ort/"));
        assertEquals("supp/ort", namer.trimRefName("//supp//ort//"));
        assertEquals("support", namer.trimRefName("support."));
        assertEquals("support", namer.trimRefName("support."));
        assertEquals("support", namer.trimRefName("support.."));
        assertEquals("support", namer.trimRefName("@{supp@{ort@{"));
        assertEquals("support", namer.trimRefName("@supp@ort@"));
        assertEquals("support", namer.trimRefName("\\supp\\ort\\"));
        assertEquals("support", namer.trimRefName("\\\\supp\\\\ort\\\\"));
    }

    @Test
    public void testGetBranchName1() {
        String prefix = "support/";
        String name = "SP1";
        String version = "1.0-1";
        String expResult = "support/SP1/1.0-1";
        String result = namer.getBranchName(prefix, name, version);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetBranchName2() {
        String prefix = "support/";
        String name = "";
        String version = "1.0-1";
        String expResult = "support/1.0-1";
        String result = namer.getBranchName(prefix, name, version);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetBranchName3() {
        String prefix = "support/";
        String name = null;
        String version = "1.0-1";
        String expResult = "support/1.0-1";
        String result = namer.getBranchName(prefix, name, version);
        assertEquals(expResult, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBranchName4() {
        String prefix = "";
        String name = "SP1";
        String version = "1.0-1";
        String expResult = "support/1.0-1";
        String result = namer.getBranchName(prefix, name, version);
        assertEquals(expResult, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBranchName5() {
        String prefix = null;
        String name = "SP1";
        String version = "1.0-1";
        String expResult = "support/1.0-1";
        String result = namer.getBranchName(prefix, name, version);
        assertEquals(expResult, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBranchName6() {
        String prefix = "support/";
        String name = "SP1";
        String version = "FooBar-Version";
        String expResult = "support/1.0-1";
        String result = namer.getBranchName(prefix, name, version);
        assertEquals(expResult, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBranchName7() {
        String prefix = "support/";
        String name = "SP1";
        String version = "";
        String expResult = "support/1.0-1";
        String result = namer.getBranchName(prefix, name, version);
        assertEquals(expResult, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBranchName8() {
        String prefix = "support/";
        String name = "SP1";
        String version = null;
        String expResult = "support/1.0-1";
        String result = namer.getBranchName(prefix, name, version);
        assertEquals(expResult, result);
    }

}
