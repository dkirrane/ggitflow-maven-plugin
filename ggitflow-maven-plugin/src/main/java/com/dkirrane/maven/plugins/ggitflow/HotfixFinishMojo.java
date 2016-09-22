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

import com.dkirrane.gitflow.groovy.GitflowHotfix;
import com.dkirrane.gitflow.groovy.ex.GitflowException;
import com.dkirrane.gitflow.groovy.ex.GitflowMergeConflictException;
import static com.dkirrane.maven.plugins.ggitflow.AbstractGitflowMojo.DEFAULT_DEPLOY_ARGS;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Merges a hotfix branch back into the develop and master branch and then
 * creates a tag for the hotfix on master.
 */
@Mojo(name = "hotfix-finish", aggregator = true)
public class HotfixFinishMojo extends AbstractHotfixMojo {

    protected String hotfixName;

    /**
     * If <code>true</code>, the hotfix finish merge to develop & master and
     * created tag will get pushed to the remote repository
     *
     * @since 1.6
     */
    @Parameter(property = "pushHotfixFinish", defaultValue = "false", required = false)
    protected boolean pushHotfixFinish;

    /**
     * If <code>true</code>, the hotfix can still finish even if
     * <code>-SNAPSHOT</code> dependencies exists in the pom.
     *
     * @since 1.2
     */
    @Parameter(property = "allowSnapshots", defaultValue = "false", required = false)
    private boolean allowSnapshots;

    /**
     * Skips any calls to <code>mvn install</code>
     *
     * @since 1.2
     */
    @Parameter(property = "skipBuild", defaultValue = "true", required = false)
    private Boolean skipBuild;

    /**
     * Skips any calls to <code>mvn deploy</code>
     *
     * @since 1.2
     */
    @Parameter(property = "skipDeploy", defaultValue = "true", required = false)
    private Boolean skipDeploy;

    /**
     * Skips any tests during <code>mvn install</code> and
     * <code>mvn deploy</code>
     *
     * @since 1.2
     */
    @Parameter(property = "skipTests", defaultValue = "true", required = false)
    private Boolean skipTests;

    /**
     * Whether to use the release profile that adds sources and javadoc to the
     * released artifact, if appropriate. If set to true, the hotfix-finish will
     * set the property "performRelease" to true, which activates the profile
     * "release-profile", which is inherited from the super pom.
     *
     * @since 1.2
     */
    @Parameter(property = "useReleaseProfile", defaultValue = "true", required = false)
    private Boolean useReleaseProfile;

    /**
     * Comma separated profiles to enable on deployment, in addition to active
     * profiles for project execution.
     *
     * @since 1.2
     */
    @Parameter(property = "releaseProfiles", defaultValue = "", required = false)
    private String releaseProfiles;

    /**
     * If <code>true</code>, all commits to the branch will be squashed into a
     * single commit before the merge.
     *
     * @since 1.2
     */
    @Parameter(property = "squash", defaultValue = "false", required = false)
    private Boolean squash;

    /**
     * If <code>true</code>, the hotfix feature branch will not be deleted after
     * the merge.
     *
     * @since 1.6
     */
    @Parameter(property = "keep", defaultValue = "false", required = false)
    private Boolean keepLocal;

    /**
     * If <code>true</code>, the hotfix feature branch will not be deleted after
     * the merge.
     *
     * @since 1.6
     */
    @Parameter(property = "keep", defaultValue = "true", required = false)
    private Boolean keepRemote;

    /**
     * If <code>true</code>, the hotfix tag will be signed.
     *
     * @since 1.2
     */
    @Parameter(property = "sign", defaultValue = "false", required = false)
    private Boolean sign;

    /**
     * The GNU Privacy Guard (GPG) private key used to sign the tag.
     *
     * @since 1.2
     */
    @Parameter(property = "signingkey", defaultValue = "", required = false)
    private String signingkey;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().debug("Finishing hotfix");

        /* Fetch any new tags and prune any branches that may already be deleted */
        getGitflowInit().executeRemote("git fetch --tags --prune");

        /* Get hotfix branch name */
        String prefix = getHotfixBranchPrefix();
        List<String> hotfixBranches = getGitflowInit().gitLocalHotfixBranches();
        if (hotfixBranches.isEmpty()) {
            throw new MojoFailureException("Could not find any local hotfix branch!");
        }

        if (StringUtils.isBlank(hotfixName)) {
            if (hotfixBranches.size() == 1) {
                String hotfixBranch = hotfixBranches.get(0);
                hotfixName = trimHotfixName(hotfixBranch);
            } else {
                String hotfixBranch = promptForExistingHotfixBranch(prefix, hotfixBranches);
                hotfixName = trimHotfixName(hotfixBranch);
            }

        } else {
            hotfixName = trimHotfixName(hotfixName);
            if (!getGitflowInit().gitLocalBranchExists(prefix + hotfixName)) {
                throw new MojoFailureException("No local hotfix branch named '" + prefix + hotfixName + "' exists!");
            }
        }

        getLog().info("Finishing hotfix '" + hotfixName + "'");

        String hotfixBranch = prefix + hotfixName;

        /* Switch to develop branch and get its current version */
        String developBranch = getGitflowInit().getDevelopBranch();
        getGitflowInit().executeLocal("git checkout " + developBranch);
        reloadReactorProjects();
        String developVersion = project.getVersion();
        getLog().debug("develop version = " + developVersion);

        /* Switch to hotfix branch and set poms to hotfix version */
        getGitflowInit().executeLocal("git checkout " + hotfixBranch);
        reloadReactorProjects();
        String hotfixVersion = project.getVersion();
        getLog().debug("hotfix snapshot version = " + hotfixVersion);
        String hotfixReleaseVersion = getReleaseVersion(hotfixVersion);
        getLog().debug("hotfix release version = " + hotfixReleaseVersion);

        boolean setVersion = setVersion(hotfixReleaseVersion, pushHotfixFinish, hotfixBranch);

        if (!allowSnapshots) {
            reloadReactorProjects();
            try {
                checkForSnapshotDependencies();
            } catch (MojoExecutionException mee) {
                // reset setVersion commit and allow user fix & push SNAPSHOT dependencies
                // but can only reset if the commit has not been pushed */
                if (!pushHotfixFinish && setVersion) {
                    getGitflowInit().executeLocal("git reset --hard HEAD~1");
                }
                throw mee;
            }
        }

        /* finish hotfix */
        GitflowHotfix gitflowHotfix = new GitflowHotfix();
        gitflowHotfix.setInit(getGitflowInit());
        gitflowHotfix.setMsgPrefix(getMsgPrefix());
        gitflowHotfix.setMsgSuffix(getMsgSuffix());
        gitflowHotfix.setPush(pushHotfixBranch);
        gitflowHotfix.setSquash(squash);
        gitflowHotfix.setKeepLocal(keepLocal);
        gitflowHotfix.setKeepRemote(keepRemote);
        gitflowHotfix.setSign(sign);
        gitflowHotfix.setSigningkey(signingkey);

        /* 1. merge to master */
        try {
            gitflowHotfix.finishToMaster(hotfixBranch, pushHotfixFinish);
        } catch (GitflowException | GitflowMergeConflictException ge) {
            throw new MojoFailureException(ge.getMessage());
        }

        /* 2. make versions in hotfix and develop branches match to avoid conflicts */
        getGitflowInit().executeLocal("git checkout " + hotfixBranch);
        reloadReactorProjects();
        boolean setDevVersion = setVersion(developVersion, pushHotfixFinish, hotfixBranch);

        /* 3. merge to develop */
        try {
            gitflowHotfix.finishToDevelop(hotfixBranch, pushHotfixFinish);
        } catch (GitflowException ge) {
            // reset setVersion commit and allow user fix whatever exception occurred
            // but can only reset if the commit has not been pushed */
            if (!pushHotfixFinish && setDevVersion) {
                getGitflowInit().executeLocal("git reset --hard HEAD~1");
            }
            throw new MojoFailureException(ge.getMessage());
        } catch (GitflowMergeConflictException gmce) {
            // If a merge conflict arises we don't want to reset setDevVersion commit
            throw new MojoFailureException(gmce.getMessage());
        }

        /* make sure we're on the develop branch */
        String currentBranch = getGitflowInit().gitCurrentBranch();
        if (!currentBranch.equals(developBranch)) {
            throw new MojoFailureException("Current branch should be " + developBranch + " but was " + currentBranch);
        }

        /* Switch to hotfix tag and deploy it */
        getGitflowInit().executeLocal("git checkout " + getGitflowInit().getVersionTagPrefix() + hotfixReleaseVersion);
        reloadReactorProjects();
        String tagVersion = project.getVersion();
        getLog().debug("tag version = " + tagVersion);

        /* install or deploy */
        if (skipDeploy == false) {
            String goals = "clean deploy";
            if (project.getDistributionManagement() != null
                    && project.getDistributionManagement().getSite() != null) {
                goals += " site-deploy";
            }

            ImmutableList.Builder<String> additionalArgs = new ImmutableList.Builder<>();
            additionalArgs.addAll(DEFAULT_DEPLOY_ARGS);
            if (skipTests) {
                additionalArgs.add("-DskipTests=true");
            }
            if (useReleaseProfile) {
                additionalArgs.add("-DperformRelease=true");
            }
            if (StringUtils.isNotBlank(releaseProfiles)) {
                Iterable<String> profiles = PROFILES_SPLITTER.split(releaseProfiles);
                additionalArgs.add("-P " + PROFILES_JOINER.join(profiles));
            }
            runGoals(goals, additionalArgs.build());
        } else if (skipBuild == false) {
            ImmutableList.Builder<String> additionalArgs = new ImmutableList.Builder<>();
            additionalArgs.addAll(DEFAULT_DEPLOY_ARGS);
            if (skipTests) {
                additionalArgs.add("-DskipTests=true");
            }
            runGoals("clean install", additionalArgs.build());
        } else {
            getLog().debug("Skipping both install and deploy for hotfix tag " + hotfixName);
        }

        getGitflowInit().executeLocal("git checkout " + developBranch);
    }

    private String promptForExistingHotfixBranch(String prefix, List<String> hotfixBranches) throws MojoFailureException {
        List<String> choices = hotfixBranches;

        /* if current branch is a feature branch at it to start of list so it is the default in prompt */
        String currentBranch = getGitflowInit().gitCurrentBranch();
        if (currentBranch.startsWith(prefix)) {
            choices = rearrange(currentBranch, hotfixBranches);
        }

        String name = "";
        try {
            prompter.promptChoice("Hotfix branches", "Please select a hotfix branch to finish", choices);
        } catch (IOException ex) {
            throw new MojoFailureException("Error reading hotfix name from command line " + ex.getMessage());
        }
        return name.trim();
    }
}
