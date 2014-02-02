/*
 * Copyright 2014 desmondkirrane.
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

import com.dkirrane.gitflow.groovy.GitflowHotfix;
import com.dkirrane.gitflow.groovy.ex.GitflowException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.hudson.util.GenericArtifactVersion;

/**
 *
 */
@Mojo(name = "hotfix-start", aggregator = true, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class HotfixStartMojo extends HotfixAbstractMojo {

    @Parameter(property = "startCommit", defaultValue = "")
    private String startCommit;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        getGitflowInit().executeLocal("git checkout " + getGitflowInit().getMasterBrnName());

        String hotfixVersion = getHotfixVersion(project.getVersion());

        getLog().info("Starting hotfix '" + hotfixVersion + "'");
        getLog().debug("msgPrefix '" + getMsgPrefix() + "'");
        getLog().debug("msgSuffix '" + getMsgSuffix() + "'");

        GitflowHotfix gitflowHotfix = new GitflowHotfix();
        gitflowHotfix.setInit(getGitflowInit());
        gitflowHotfix.setMsgPrefix(getMsgPrefix());
        gitflowHotfix.setMsgSuffix(getMsgSuffix());

        if (!StringUtils.isEmpty(startCommit)) {
            gitflowHotfix.setStartCommit(startCommit);
        }

        try {
            gitflowHotfix.start(hotfixVersion);
        } catch (GitflowException ge) {
            throw new MojoFailureException(ge.getMessage());
        }
    }

    private String getHotfixVersion(String currentVersion) throws MojoFailureException {
        getLog().info("Project version '" + currentVersion + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(currentVersion);

        artifactVersion.upgradeLeastSignificantNumber();

        return artifactVersion.toString();
    }
}
