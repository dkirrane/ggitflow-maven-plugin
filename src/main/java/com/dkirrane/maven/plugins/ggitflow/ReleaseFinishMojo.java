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

import com.dkirrane.gitflow.groovy.GitflowFeature;
import com.dkirrane.gitflow.groovy.ex.GitflowException;
import com.dkirrane.gitflow.groovy.ex.GitflowMergeConflictException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.List;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 *
 */
@Mojo(name = "release-finish", aggregator = true)
public class ReleaseFinishMojo extends AbstractReleaseMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().info("Finishing release '" + releaseName + "'");

        List<String> releaseBranches = getGitflowInit().gitLocalReleaseBranches();

        if (releaseBranches.isEmpty()) {
            throw new MojoFailureException("Could not find release branch!");
        }

        if (releaseBranches.size() == 1) {
            releaseName = releaseBranches.get(1);
        } else {
            releaseName = promptForExistingReleaseName(releaseBranches, releaseName);
        }

        //make sure we're on the release branch
        if (!getGitflowInit().gitCurrentBranch().equals(releaseName)) {
            getGitflowInit().executeLocal("git checkout -b " + releaseName);
        }

        getLog().info("START org.codehaus.mojo:versions-maven-plugin:2.1:set '" + releaseName + "'");
        executeMojo(
                plugin(
                        groupId("org.codehaus.mojo"),
                        artifactId("versions-maven-plugin"),
                        version("2.1")
                ),
                goal("set"),
                configuration(
                        element(name("generateBackupPoms"), "false"),
                        element(name("newVersion"), releaseName)
                ),
                executionEnvironment(
                        project,
                        session,
                        pluginManager
                )
        );
        getLog().info("DONE org.codehaus.mojo:versions-maven-plugin:2.1:set '" + releaseName + "'");

        if (!getGitflowInit().gitIsCleanWorkingTree()) {
            String msg = getMsgPrefix() + "Updating poms for " + releaseName + " branch" + getMsgSuffix();
            getGitflowInit().executeLocal("git add -A .");
            String[] cmtPom = {"git", "commit", "-m", "\"" + msg + "\""};
            getGitflowInit().executeLocal(cmtPom);
        } else {
            getLog().error("Failed to update poms to the release version " + releaseName);
        }

        GitflowFeature gitflowFeature = new GitflowFeature();
        gitflowFeature.setInit(getGitflowInit());
        gitflowFeature.setMsgPrefix(getMsgPrefix());
        gitflowFeature.setMsgSuffix(getMsgSuffix());

        try {
            gitflowFeature.finish(releaseName);
        } catch (GitflowException ge) {
            throw new MojoFailureException(ge.getMessage());
        } catch (GitflowMergeConflictException gmce) {
            throw new MojoFailureException(gmce.getMessage());
        }

        //make sure we're on the develop branch
        String currentBranch = getGitflowInit().gitCurrentBranch();
        String developBrnName = (String) getGitflowInit().getDevelopBrnName();
        if (!currentBranch.equals(developBrnName)) {
            getLog().error("Current branch should be " + developBrnName + " but was " + currentBranch);
            getGitflowInit().executeLocal("git checkout -b " + developBrnName);
        }

        // @todo we should merge ours the release branch into develop
    }

    private String promptForExistingReleaseName(List<String> releaseBranches, String defaultReleaseName) throws MojoFailureException {
        String message = "Please select a release branch to finish?";

        String name = "";
        try {
            name = prompter.prompt(message, releaseBranches, defaultReleaseName);
        } catch (PrompterException e) {
            throw new MojoFailureException("Error reading release name from command line " + e.getMessage());
        }

        return name;
    }

}
