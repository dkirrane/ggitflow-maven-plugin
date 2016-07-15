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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Mojo used for Testing
 */
//@Mojo(name = "debug-start", aggregator = true, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class DebuggingMojo extends AbstractGitflowMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        reloadReactorProjects();

        setVersion("1.4", false, "someBranchName");

        runGoals("clean deploy", DEFAULT_DEPLOY_ARGS);
    }
}
