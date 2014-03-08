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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.hudson.util.GenericArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a new release branch off of the develop branch.
 */
@Mojo(name = "release-start", aggregator = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class ReleaseStartMojo extends AbstractReleaseMojo {

    private static final Logger LOG = LoggerFactory.getLogger(ReleaseStartMojo.class.getName());

    /**
     * Any dependencies with a <code>-SNAPSHOT</code> version are replaced with
     * the corresponding release version (if it has been released). This action
     * is performed on the release branch after it is created.
     *
     * @since 1.2
     */
    @Parameter(property = "useReleases", defaultValue = "true", required = false)
    private boolean useReleases;

    /**
     * The commit to start the release branch from.
     *
     * @since 1.2
     */
    @Parameter(property = "startCommit", defaultValue = "", required = false)
    private String startCommit;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        /* Switch to develop branch and get its current version */
        getGitflowInit().executeLocal("git checkout " + getGitflowInit().getDevelopBranch());
        reloadReactorProjects();
        String developVersion = project.getVersion();
        LOG.debug("develop version = " + developVersion);

        /* Get suggested release version */
        String releaseVersion = getReleaseVersion(developVersion);
        LOG.debug("release version = " + releaseVersion);
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

        LOG.info("Starting release '" + releaseName + "'");
        LOG.debug("msgPrefix '" + getMsgPrefix() + "'");
        LOG.debug("msgSuffix '" + getMsgSuffix() + "'");

        GitflowRelease gitflowRelease = new GitflowRelease();
        gitflowRelease.setInit(getGitflowInit());
        gitflowRelease.setMsgPrefix(getMsgPrefix());
        gitflowRelease.setMsgSuffix(getMsgSuffix());
        gitflowRelease.setPush(pushReleases);
        gitflowRelease.setStartCommit(startCommit);

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

        /* Update dependencies to release version */
        if (useReleases) {
            reloadReactorProjects();
            setNextVersions(false);
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
