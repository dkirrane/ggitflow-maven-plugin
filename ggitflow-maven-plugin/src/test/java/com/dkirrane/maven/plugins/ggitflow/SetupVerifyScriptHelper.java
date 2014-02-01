/* 
 * Copyright 2014 Desmond Kirrane.
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
package com.dkirrane.maven.plugins.ggitflow;

import com.dkirrane.gitflow.groovy.GitflowInit;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.codehaus.plexus.util.FileUtils;

/**
 *
 */
public class SetupVerifyScriptHelper {

    public static final String BUILD_LOG_FILE = "build.log";

    /**
     * The absolute path to the base directory of the test project.
     */
    protected File baseDirectory;

    /**
     * The absolute path to the local repository used for the Maven invocation
     * on the test project.
     */
    protected File localRepositoryPath;

    /**
     * The storage of key-value pairs used to pass data from the pre-build hook
     * script to the post-build hook script.
     */
    protected Map context;

    public SetupVerifyScriptHelper(File baseDirectory, File localRepositoryPath, Map context) {
        this.baseDirectory = baseDirectory;
        System.out.println("baseDirectory = " + baseDirectory);
        this.localRepositoryPath = localRepositoryPath;
        System.out.println("localRepositoryPath = " + localRepositoryPath);
        this.context = context;
        System.out.println("context = " + context);
    }

    public void setUp() throws IOException {
//
//        File pluginXml = new File(getBasedir(), "src/test/resources/test-case1/plugin.xml");
//        testPom = new File(repoDir, "pom.xml");
//        FileUtils.copyFile(pluginXml, testPom);
//
//        assertTrue(testPom.exists());

        GitflowInit init = new GitflowInit();
        init.setRepoDir(this.baseDirectory);
        init.executeLocal("git init");
        init.executeLocal("git add -A .");
        String[] cmtPom = {"git", "commit", "-m", "\"Commit pom.xml\""};
        init.executeLocal(cmtPom);

//        init.executeLocal("/usr/local/bin/stree " + this.baseDirectory.getCanonicalPath());
    }

    public void tearDown() {
        baseDirectory.deleteOnExit();
    }

    public void assertBuildLogContains(String search) throws Exception {
        assertFileContains(BUILD_LOG_FILE, search);
    }

    public void assertFileContains(String path, String search) throws Exception {
        if (!FileUtils.fileRead(new File(baseDirectory, path)).contains(search)) {
            throw new Exception(path + " does not contain '" + search + "'.");
        }
    }

    public void featureBranchExists(String branchName) throws Exception {
        GitflowInit init = new GitflowInit();
        init.setRepoDir(this.baseDirectory);
        Boolean exists = init.gitLocalBranchExists(init.getFeatureBranchPrefix() + branchName);
        if (!exists) {
            throw new Exception("Feature branch does not exist " + branchName);
        }
    }
}
