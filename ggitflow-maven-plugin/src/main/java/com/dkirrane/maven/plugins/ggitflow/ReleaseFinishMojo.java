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
import com.dkirrane.gitflow.groovy.ex.GitflowMergeConflictException;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.jfrog.hudson.util.GenericArtifactVersion;

/**
 * Merges a release branch back into the develop and master branch and then
 * creates a tag for the release on master.
 */
@Mojo(name = "release-finish", aggregator = true)
public class ReleaseFinishMojo extends AbstractReleaseMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().info("Finishing release '" + releaseName + "'");

        /* Get release branch name */
        List<String> releaseBranches = getGitflowInit().gitLocalReleaseBranches();
        if (releaseBranches.isEmpty()) {
            throw new MojoFailureException("Could not find release branch!");
        } else if (releaseBranches.size() == 1) {
            releaseName = releaseBranches.get(0);
        } else {
            releaseName = promptForExistingReleaseName(releaseBranches, releaseName);
        }

        /* Switch to develop branch and get its current version */
        String developBranch = getGitflowInit().getDevelopBranch();
        getGitflowInit().executeLocal("git checkout " + developBranch);
        reloadReactorProjects();
        String developVersion = project.getVersion();
        getLog().debug("develop version = " + developVersion);

        /* Switch to release branch and set poms to release version */
        getGitflowInit().executeLocal("git checkout " + releaseName);
        reloadReactorProjects();
        String releaseVersion = getReleaseVersion(project.getVersion());
        getLog().debug("release version = " + releaseVersion);

        setVersion(releaseVersion);

        /* finish release */
        GitflowRelease gitflowRelease = new GitflowRelease();
        gitflowRelease.setInit(getGitflowInit());
        gitflowRelease.setMsgPrefix(getMsgPrefix());
        gitflowRelease.setMsgSuffix(getMsgSuffix());
        gitflowRelease.setSquash(squash);
        gitflowRelease.setKeep(keep);
        gitflowRelease.setSign(sign);
        gitflowRelease.setSigningkey(signingkey);

        try {
            gitflowRelease.finish(releaseName);
        } catch (GitflowException ge) {
            throw new MojoFailureException(ge.getMessage());
        } catch (GitflowMergeConflictException gmce) {
            throw new MojoFailureException(gmce.getMessage());
//            FixPomMergeConflicts fixPomMergeConflicts = new FixPomMergeConflicts();
//            try {
//                fixPomMergeConflicts.resolveConflicts2();
//            } catch (GitflowException ge) {
//                throw new MojoFailureException(ge.getMessage());
//            } catch (GitflowMergeConflictException gmce2) {
//                throw new MojoFailureException(gmce2.getMessage());
//            }
        }

        /* make sure we're on the develop branch */
        String currentBranch = getGitflowInit().gitCurrentBranch();
        if (!currentBranch.equals(developBranch)) {
            throw new MojoFailureException("Current branch should be " + developBranch + " but was " + currentBranch);
        }
        /* set poms to next develop version */
        String nextDevelopVersion = getNextDevelopVersion(developVersion);
        reloadReactorProjects();
        setVersion(nextDevelopVersion);

        /* Switch to release tag and deploy it */
        getGitflowInit().executeLocal("git checkout " + getGitflowInit().getVersionTagPrefix() + releaseVersion);
        reloadReactorProjects();
        String tagVersion = project.getVersion();
        getLog().debug("tag version = " + tagVersion);

        /* install or deploy */
        if (skipDeploy == false) {
            clean();
            deploy();
        } else if (skipBuild == false) {
            clean();
            install();
        } else {
            getLog().debug("Skipping both install and deploy for tag " + tagVersion);
        }
        getGitflowInit().executeLocal("git checkout " + developBranch);
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

    private String getNextDevelopVersion(String developVersion) {
        getLog().debug("Current Develop version '" + developVersion + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(developVersion);
        artifactVersion.upgradeLeastSignificantPrimaryNumber();

        return artifactVersion.toString();
    }

}
