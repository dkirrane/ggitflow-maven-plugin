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

import com.dkirrane.gitflow.groovy.GitflowRelease;
import com.dkirrane.gitflow.groovy.ex.GitflowException;
import com.dkirrane.maven.plugins.ggitflow.util.MavenUtil;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.hudson.util.GenericArtifactVersion;

/**
 *
 */
@Mojo(name = "release-start", aggregator = true, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class ReleaseStartMojo extends AbstractReleaseMojo {

    @Parameter(property = "startCommit", defaultValue = "")
    private String startCommit;

    @Parameter(defaultValue = "false", property = "allowSnapshots")
    private boolean allowSnapshots = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        /* Switch to develop branch and get its current version */
        getGitflowInit().executeLocal("git checkout " + getGitflowInit().getDevelopBranch());
        reloadReactorProjects();
        String developVersion = project.getVersion();
        getLog().info("develop version = " + developVersion);

        /* Get suggested release version */
        String releaseVersion = getReleaseVersion(developVersion);
        getLog().info("release version = " + releaseVersion);
//        String nextDevelopmentVersion = getNextDevelopmentVersion(project.getVersion());

        /* create release branch */
        String prefix = getReleaseBranchPrefix();
        if (StringUtils.isBlank(releaseName)) {
            String message = "What is the release branch name? " + prefix;
            try {
                releaseName = prompter.prompt(message, releaseVersion);
                if (StringUtils.isBlank(releaseName)) {
                    throw new MojoFailureException("Parameter <releaseName> cannot be null or empty.");
                }
            } catch (PrompterException ex) {
                throw new MojoExecutionException("Error reading release name from command line " + ex.getMessage(), ex);
            }
        }

        try {
            new GenericArtifactVersion(releaseName);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Provided releaseName " + releaseName + " is not a valid Maven version.");
        }

        getLog().info("Starting release '" + releaseName + "'");
        getLog().info("msgPrefix '" + getMsgPrefix() + "'");
        getLog().info("msgSuffix '" + getMsgSuffix() + "'");

        GitflowRelease gitflowRelease = new GitflowRelease();
        gitflowRelease.setInit(getGitflowInit());
        gitflowRelease.setMsgPrefix(getMsgPrefix());
        gitflowRelease.setMsgSuffix(getMsgSuffix());

        try {
            gitflowRelease.start(releaseName);
        } catch (GitflowException ge) {
            throw new MojoFailureException(ge.getMessage());
        }

        // current branch should be the release branch
        String releaseBranch = getGitflowInit().gitCurrentBranch();
        if (!releaseBranch.startsWith(prefix)) {
            throw new MojoFailureException("Failed to create release version.");
        }

//        // checkout develop branch and update it's version
//        String developBranch = (String) getGitflowInit().getDevelopBrnName();
//        getGitflowInit().executeLocal("git checkout " + developBranch);
//        setVersion(nextDevelopmentVersion);
//
//        // checkout release branch again
//        getGitflowInit().executeLocal("git checkout " + releaseBranch);
    }

    public String getReleaseName() {
        return releaseName;
    }
}
