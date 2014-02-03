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
import com.dkirrane.gitflow.groovy.ex.GitflowMergeConflictException;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.components.interactivity.PrompterException;

/**
 *
 */
@Mojo(name = "hotfix-finish", aggregator = true, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class HotfixFinishMojo extends HotfixAbstractMojo {

    protected String hotfixName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        List<String> hotfixBranches = getGitflowInit().gitLocalHotfixBranches();

        if (hotfixBranches.isEmpty()) {
            throw new MojoFailureException("Could not find release branch!");
        }

        if (hotfixBranches.size() == 1) {
            hotfixName = hotfixBranches.get(0);
        } else {
            hotfixName = promptForExistingHotfixName(hotfixBranches, hotfixName);
        }

        String hotfixReleaseVersion = getReleaseVersion(project.getVersion());
        setVersion(hotfixReleaseVersion);

        GitflowHotfix gitflowHotfix = new GitflowHotfix();
        gitflowHotfix.setInit(getGitflowInit());
        gitflowHotfix.setMsgPrefix(getMsgPrefix());
        gitflowHotfix.setMsgSuffix(getMsgSuffix());

        try {
            gitflowHotfix.finish(hotfixName);
        } catch (GitflowException ge) {
            throw new MojoFailureException(ge.getMessage());
        } catch (GitflowMergeConflictException gmce) {
            throw new MojoFailureException(gmce.getMessage());
        }
        
        if (skipDeploy == false) {
            // checkout and deploy the hotfix tag
            getGitflowInit().executeLocal("git checkout " + getGitflowInit().getVersionTagPrefix() + hotfixName);            
            clean();
            deploy();
        } else if (skipBuild == false) {
            // checkout and install the hotfix tag
            getGitflowInit().executeLocal("git checkout " + getGitflowInit().getVersionTagPrefix() + hotfixName);            
            clean();
            install();
        } else {
            getLog().debug("Skipping both install and deploy");
        }        
    }

    private String promptForExistingHotfixName(List<String> hotfixBranches, String defaultHotfixName) throws MojoFailureException {
        String message = "Please select a hotfix branch to finish?";

        String name = "";
        try {
            name = prompter.prompt(message, hotfixBranches, defaultHotfixName);
        } catch (PrompterException e) {
            throw new MojoFailureException("Error reading hotfix name from command line " + e.getMessage());
        }

        return name;
    }
}
