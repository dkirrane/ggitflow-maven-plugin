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

import static org.jfrog.hudson.util.GenericArtifactVersion.SNAPSHOT_QUALIFIER;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.hudson.util.GenericArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dkirrane.gitflow.groovy.GitflowRelease;
import com.dkirrane.gitflow.groovy.ex.GitflowException;

/**
 * Creates a new release branch off of the develop branch.
 */
@Mojo(name = "release-start", aggregator = true)
public class ReleaseStartMojo extends AbstractReleaseMojo {

    private static final Logger LOG = LoggerFactory.getLogger(ReleaseStartMojo.class.getName());

    /**
     * Whether to run the plugin in interactive mode or not. The default is to
     * run without interaction when possible.
     *
     * @since 1.2
     */
    @Parameter(property = "interactive", defaultValue = "true", required = false)
    protected boolean interactive;

    /**
     * If the project has a parent with a <code>-SNAPSHOT</code> version it will
     * be replaced with the corresponding release version (if it has been
     * released). This action is performed on the release branch after it is
     * created.
     *
     * @since 1.2
     */
    @Parameter(property = "updateParent", defaultValue = "false", required = false)
    private boolean updateParent;

    /**
     * Any dependencies with a <code>-SNAPSHOT</code> version are replaced with
     * the corresponding release version (if it has been released). This action
     * is performed on the release branch after it is created.
     *
     * @since 1.2
     */
    @Parameter(property = "updateDependencies", defaultValue = "false", required = false)
    private boolean updateDependencies;

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
        LOG.info("Current develop version = " + developVersion);

        /* Get next development version */
        String nextDevelopVersion = getNextDevelopVersion(developVersion);
        if (interactive) {
            String message = "What is the next development version? ";
            try {
                nextDevelopVersion = prompter.prompt(message, nextDevelopVersion);
            } catch (PrompterException ex) {
                throw new MojoExecutionException("Error reading next development version from command line " + ex.getMessage(), ex);
            }
        }      
        LOG.debug("next develop version = " + developVersion);

        /* Get suggested release version */
        String releaseVersion = getReleaseVersion(developVersion);
        LOG.debug("release version = " + releaseVersion);

        /* create release branch */
        String prefix = getReleaseBranchPrefix();
        if (interactive && StringUtils.isBlank(releaseName)) {
            String message = "What is the release branch name? " + prefix;
            try {
                releaseName = prompter.prompt(message, releaseVersion);
            } catch (PrompterException ex) {
                throw new MojoExecutionException("Error reading release name from command line " + ex.getMessage(), ex);
            }
        } else {
            releaseName = releaseVersion;
        }

        if (StringUtils.isBlank(releaseName)) {
            throw new MojoFailureException("Parameter <releaseName> cannot be null or empty.");
        }

        GenericArtifactVersion releaseArtifactVersion;
        try {
            releaseArtifactVersion = new GenericArtifactVersion(releaseName);
            releaseArtifactVersion.setBuildSpecifier(SNAPSHOT_QUALIFIER);
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

        /* Update release branch dependencies to release version */
        if (updateDependencies) {
            reloadReactorProjects();
            setNextVersions(false, updateParent);
        }

        // checkout develop branch and update it's version
        String developBranch = (String) getGitflowInit().getDevelopBrnName();
        getGitflowInit().executeLocal("git checkout " + developBranch);
        reloadReactorProjects();
        setVersion(nextDevelopVersion);

        // checkout release branch again
        getGitflowInit().executeLocal("git checkout " + releaseBranch);
        reloadReactorProjects();
        setVersion(releaseArtifactVersion.toString());
    }

    public String getReleaseName() {
        return releaseName;
    }

    private String getNextDevelopVersion(String developVersion) {
        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(developVersion);
        artifactVersion.upgradeLeastSignificantPrimaryNumber();

        return artifactVersion.toString();
    }
}
