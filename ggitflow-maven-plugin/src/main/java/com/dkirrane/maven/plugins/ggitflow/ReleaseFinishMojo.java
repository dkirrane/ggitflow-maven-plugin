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
import com.dkirrane.maven.plugins.ggitflow.util.MavenUtil;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.hudson.util.GenericArtifactVersion;

/**
 *
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

        /* Switch to develop branch and get current develop version */
        String developBranch = getGitflowInit().getDevelopBranch();
        getGitflowInit().executeLocal("git checkout " + developBranch);
        Model devModel = MavenUtil.readPom(reactorProjects);
        String developVersion = devModel.getVersion();

        /* Switch to release branch and set poms to release version */
        getGitflowInit().executeLocal("git checkout " + releaseName);
        Model relModel = MavenUtil.readPom(reactorProjects);
        String relVersion = relModel.getVersion();
        String releaseVersion = getReleaseVersion(relVersion);
        setVersion(releaseVersion);

        /* finish release */
        GitflowRelease gitflowRelease = new GitflowRelease();
        gitflowRelease.setInit(getGitflowInit());
        gitflowRelease.setMsgPrefix(getMsgPrefix());
        gitflowRelease.setMsgSuffix(getMsgSuffix());

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
        setVersion(nextDevelopVersion);

        /* install or deploy */
        if (skipDeploy == false) {
            /* checkout and deploy the release tag */
            getGitflowInit().executeLocal("git checkout " + getGitflowInit().getVersionTagPrefix() + releaseVersion);
            String currBranch = getGitflowInit().gitCurrentBranch();
            System.out.println("currBranch = " + currBranch);
            Model tagModel = MavenUtil.readPom(reactorProjects);
            String tagVersion = tagModel.getVersion();
            System.out.println("tagVersion = " + tagVersion);
            clean();
            deploy();
        } else if (skipBuild == false) {
            /* checkout and install the release tag */
            getGitflowInit().executeLocal("git checkout " + getGitflowInit().getVersionTagPrefix() + releaseVersion);
            String currBranch = getGitflowInit().gitCurrentBranch();
            System.out.println("currBranch = " + currBranch);
            Model tagModel = MavenUtil.readPom(reactorProjects);
            String tagVersion = tagModel.getVersion();
            System.out.println("tagVersion = " + tagVersion);
            clean();
            install();
        } else {
            getLog().debug("Skipping both install and deploy");
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
        getLog().info("Current Develop version '" + developVersion + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(developVersion);
        artifactVersion.upgradeLeastSignificantPrimaryNumber();

        return artifactVersion.toString();
    }

}
