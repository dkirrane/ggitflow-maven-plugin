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
import com.dkirrane.gitflow.groovy.ex.GitCommandException;
import com.dkirrane.gitflow.groovy.ex.GitflowException;
import com.dkirrane.gitflow.groovy.ex.GitflowMergeConflictException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Merges a feature branch back into the develop.
 */
@Mojo(name = "feature-finish", aggregator = true)
public class FeatureFinishMojo extends AbstractFeatureMojo {

    /**
     * If <code>true</code>, the feature branch will be rebased onto develop,
     * then finish will fast-forward merge the feature branch back onto develop.
     *
     * @since 1.2
     */
    @Parameter(property = "isRebase", defaultValue = "false", required = false)
    private boolean isRebase;

//    No good way to handle Interactive rebase from a Maven plugin
//    /**
//     * If <code>true</code>, and <code>isRebase</code> is true, then an interactive rebase is
//     * performed for the feature branch.
//     *
//     * @since 1.2
//     */
//    @Parameter(property = "isInteractive", defaultValue = "false", required = false)
    private final boolean isInteractive = false;

    /**
     * If <code>true</code>, all commits to the branch will be squashed into a
     * single commit before the merge.
     *
     * @since 1.2
     */
    @Parameter(property = "squash", defaultValue = "false", required = false)
    private Boolean squash;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().debug("Finishing feature");

        String prefix = getFeatureBranchPrefix();
        List<String> featureBranches = getGitflowInit().gitLocalFeatureBranches();
        if (null == featureBranches || featureBranches.isEmpty()) {
            throw new MojoFailureException("No local feature branches exist!");
        }

        if (StringUtils.isBlank(featureName)) {
            String featureBranch = promptForExistingFeatureBranch(prefix, featureBranches);
            featureName = trimFeatureName(featureBranch);
        } else {
            featureName = trimFeatureName(featureName);
            if (!getGitflowInit().gitLocalBranchExists(prefix + featureName)) {
                throw new MojoFailureException("No local feature branch named '" + prefix + featureName + "' exists!");
            }
        }

        getLog().info("Finishing feature '" + featureName + "'");

        String featureBranch = prefix + featureName;
        String developBranch = getGitflowInit().getDevelopBranch();
        String masterBranch = getGitflowInit().getMasterBranch();
        String origin = getGitflowInit().getOrigin();

        if (enableFeatureVersions) {
            /* Switch to develop branch and get its current version */
            getGitflowInit().executeLocal("git checkout " + developBranch);
            reloadReactorProjects();
            String developVersion = project.getVersion();
            getLog().debug("develop version = " + developVersion);

            /* Switch to feature branch and get its current version */
            getGitflowInit().executeLocal("git checkout " + featureBranch);
            reloadReactorProjects();
            String featureVersion = project.getVersion();
            getLog().debug("feature version = " + featureVersion);

            setVersion(developVersion, featureBranch, false);
        }

        GitflowFeature gitflowFeature = new GitflowFeature();
        gitflowFeature.setInit(getGitflowInit());
        gitflowFeature.setMsgPrefix(getMsgPrefix());
        gitflowFeature.setMsgSuffix(getMsgSuffix());
        gitflowFeature.setPush(false);
        gitflowFeature.setSquash(squash);
        gitflowFeature.setIsRebase(isRebase);
        gitflowFeature.setIsInteractive(isInteractive);

        try {
            gitflowFeature.finish(featureName);
        } catch (GitCommandException gce) {
            String header = "Error merging branch '" + featureBranch + "' into '" + masterBranch + "'";
            exceptionMapper.handle(header, gce);
        } catch (GitflowException ge) {
            String header = "Error merging branch '" + featureBranch + "' into '" + masterBranch + "'";
            exceptionMapper.handle(header, ge);
        } catch (GitflowMergeConflictException gmce) {
            String header = "Merge conflict merging branch '" + featureBranch + "' into '" + masterBranch + "'";
            exceptionMapper.handle(header, gmce);
        }


        /* make sure we're on the develop branch */
        String currentBranch = getGitflowInit().gitCurrentBranch();
        if (!currentBranch.equals(developBranch)) {
            throw new MojoFailureException("Current branch should be " + developBranch + " but was " + currentBranch);
        }

        /* Push merges and tag */
        try {
            if (session.getRequest().isInteractiveMode()) {
                prompter.pushPrompt("Are you ready to push?", Collections.EMPTY_LIST, Arrays.asList(developBranch), Arrays.asList(featureBranch, origin + '/' + featureBranch));
                boolean yes;
                try {
                    yes = prompter.promptYesNo("Do you want to continue");
                } catch (IOException e) {
                    throw new MojoFailureException("Error reading user input from command line " + e.getMessage());
                }

                if (yes) {
                    gitflowFeature.publish(featureBranch, true);
                } else {
                    gitflowFeature.publish(featureBranch, false);
                }
            } else {
                gitflowFeature.publish(featureBranch, true);
            }
        } catch (GitCommandException gce) {
            String header = "Failed to push release finish";
            exceptionMapper.handle(header, gce);
        } catch (GitflowException ge) {
            String header = "Failed to push release finish";
            exceptionMapper.handle(header, ge);
        }
    }

    private String promptForExistingFeatureBranch(String prefix, List<String> featureBranches) throws MojoFailureException {
        List<String> choices = featureBranches;

        /* if current branch is a feature branch at it to start of list so it is the default in prompt */
        String currentBranch = getGitflowInit().gitCurrentBranch();
        if (currentBranch.startsWith(prefix)) {
            choices = rearrange(currentBranch, featureBranches);
        }

        String name = "";
        try {
            prompter.promptChoice("Feature branches", "Please select a feature branch to finish", choices);
        } catch (IOException ex) {
            throw new MojoFailureException("Error reading feature name from command line " + ex.getMessage());
        }
        return name.trim();
    }

}
