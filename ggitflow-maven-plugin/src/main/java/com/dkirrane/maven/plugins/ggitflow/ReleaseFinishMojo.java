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

/**
 *
 */
@Mojo(name = "release-finish", aggregator = true)
public class ReleaseFinishMojo extends AbstractReleaseMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().info("Finishing release '" + releaseName + "'");

        // checkout develop branch to get it's version
//        String developBranch = (String) getGitflowInit().getDevelopBrnName();
//        getGitflowInit().executeLocal("git checkout " + developBranch);
//        String developVersion = project.getVersion();
        List<String> releaseBranches = getGitflowInit().gitLocalReleaseBranches();

        if (releaseBranches.isEmpty()) {
            throw new MojoFailureException("Could not find release branch!");
        }

        if (releaseBranches.size() == 1) {
            releaseName = releaseBranches.get(0);
        } else {
            releaseName = promptForExistingReleaseName(releaseBranches, releaseName);
        }

        // 1. make sure we're on the release branch
        if (!getGitflowInit().gitCurrentBranch().equals(releaseName)) {
            getGitflowInit().executeLocal("git checkout " + releaseName);
        }

        String releaseVersion = getReleaseVersion(project.getVersion());

        // @todo we should run clean install first
        // 2. update poms to release version
        setVersion(releaseVersion);

        // 4. finish feature
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

        //make sure we're on the develop branch
        String currentBranch = getGitflowInit().gitCurrentBranch();
        String developBrnName = (String) getGitflowInit().getDevelopBrnName();
        if (!currentBranch.equals(developBrnName)) {
            throw new MojoFailureException("Current branch should be " + developBrnName + " but was " + currentBranch);
        }
      
        System.out.println("skipDeploy = " + skipDeploy);
        System.out.println("skipBuild = " + skipBuild);
        if (skipDeploy == false) {
            // checkout and deploy the release tag
            getGitflowInit().executeLocal("git checkout " + getGitflowInit().getVersionTagPrefix() + releaseVersion);            
            clean();
            deploy();
        } else if (skipBuild == false) {
            // checkout and install the release tag
            getGitflowInit().executeLocal("git checkout " + getGitflowInit().getVersionTagPrefix() + releaseVersion);            
            clean();
            install();
        } else {
            getLog().debug("Skipping both install and deploy");
        }
//        getGitflowInit().executeLocal("git checkout " + getGitflowInit().getVersionTagPrefix() + releaseVersion); 
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
