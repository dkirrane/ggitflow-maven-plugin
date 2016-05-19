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
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Merges a feature branch back into the develop.
 */
@Mojo(name = "feature-finish", aggregator = true)
public class FeatureFinishMojo extends AbstractFeatureMojo {

    /**
     * If <code>true</code>, the feature finish merge to develop will get pushed
     * to the remote repository
     *
     * @since 1.6
     */
    @Parameter(property = "pushFeatureFinish", defaultValue = "false", required = false)
    protected boolean pushFeatureFinish;

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
     * Skips any calls to <code>mvn install</code>
     *
     * @since 1.2
     */
    @Parameter(property = "skipBuild", defaultValue = "true", required = false)
    private Boolean skipBuild;

    /**
     * Skips any tests during <code>mvn install</code>
     *
     * @since 1.2
     */
    @Parameter(property = "skipTests", defaultValue = "true", required = false)
    private Boolean skipTests;

    /**
     * If <code>true</code>, all commits to the branch will be squashed into a
     * single commit before the merge.
     *
     * @since 1.2
     */
    @Parameter(property = "squash", defaultValue = "false", required = false)
    private Boolean squash;

    /**
     * If <code>true</code>, the branch will not be deleted after the merge.
     *
     * @since 1.2
     */
    @Parameter(property = "keep", defaultValue = "false", required = false)
    private Boolean keep;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().debug("Finishing feature");

        List<String> featureBranches = getGitflowInit().gitLocalFeatureBranches();

        if (null == featureBranches || featureBranches.isEmpty()) {
            throw new MojoFailureException("No local feature branches exist!");
        }

        String featureBranchPrefix = getFeatureBranchPrefix();
        if (StringUtils.isBlank(featureName)) {
            String currentBranch = getGitflowInit().gitCurrentBranch();
            if (currentBranch.startsWith(featureBranchPrefix)) {
                featureName = currentBranch;
            } else {
                featureName = featureBranches.get(0);
            }
        } else {
            featureName = getFeatureName(featureName);
            if (!featureName.startsWith(featureBranchPrefix)) {
                featureName = featureBranchPrefix + featureName;
            }
            if(!getGitflowInit().gitLocalBranchExists(featureName)){
                throw new MojoFailureException("No local feature branch named '" + featureName + "' exists!");
            }            
        }

        featureName = promptForExistingFeatureName(featureBranches, featureName);

        getLog().info("Finishing feature '" + featureName + "'");

        if (enableFeatureVersions) {
            /* Switch to develop branch and get its current version */
            getGitflowInit().executeLocal("git checkout " + getGitflowInit().getDevelopBranch());
            reloadReactorProjects();
            String developVersion = project.getVersion();
            getLog().debug("develop version = " + developVersion);

            /* Switch to feature branch and get its current version */
            getGitflowInit().executeLocal("git checkout " + featureName);
            reloadReactorProjects();
            String featureVersion = project.getVersion();
            getLog().debug("feature version = " + featureVersion);

            setVersion(developVersion, pushFeatureFinish);
        }

        if (skipBuild == false) {
            ImmutableList.Builder<String> additionalArgs = new ImmutableList.Builder<String>();
            additionalArgs.addAll(DEFAULT_INSTALL_ARGS);
            if (skipTests) {
                additionalArgs.add("-DskipTests=true");
            }
            runGoals("clean install", additionalArgs.build());
        } else {
            getLog().debug("Skipping mvn install for feature " + featureName);
        }

        GitflowFeature gitflowFeature = new GitflowFeature();
        gitflowFeature.setInit(getGitflowInit());
        gitflowFeature.setMsgPrefix(getMsgPrefix());
        gitflowFeature.setMsgSuffix(getMsgSuffix());
        gitflowFeature.setPush(pushFeatureBranch);
        gitflowFeature.setSquash(squash);
        gitflowFeature.setKeep(keep);
        gitflowFeature.setIsRebase(isRebase);
        gitflowFeature.setIsInteractive(isInteractive);

        try {
            gitflowFeature.finish(featureName);
        } catch (GitflowException ge) {
            throw new MojoFailureException(ge.getMessage());
        } catch (GitflowMergeConflictException gmce) {
            throw new MojoFailureException(gmce.getMessage());
        }
    }

    private String promptForExistingFeatureName(List<String> featureBranches, String defaultFeatureName) throws MojoFailureException {
        String message = "Please select a feature branch to finish?";

        String name = "";
        try {
            name = prompter.prompt(message, featureBranches, defaultFeatureName);
        } catch (PrompterException e) {
            throw new MojoFailureException("Error reading feature name from command line " + e.getMessage());
        }

        return name.trim();
    }

}
