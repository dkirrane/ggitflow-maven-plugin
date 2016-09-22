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
import static com.dkirrane.maven.plugins.ggitflow.AbstractGitflowMojo.DEFAULT_DEPLOY_ARGS;
import static com.dkirrane.maven.plugins.ggitflow.AbstractGitflowMojo.PROFILES_SPLITTER;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.hudson.util.GenericArtifactVersion;

/**
 * Merges a release branch back into the develop and master branch and then
 * creates a tag for the release on master.
 */
@Mojo(name = "release-finish", aggregator = true)
public class ReleaseFinishMojo extends AbstractReleaseMojo {

    /**
     * If <code>true</code>, the release finish merge to develop & master and
     * the created tag will get pushed to the remote repository
     *
     * @since 1.6
     */
    @Parameter(property = "pushReleaseFinish", defaultValue = "false", required = false)
    protected boolean pushReleaseFinish;

    /**
     * If <code>true</code>, the release can still finish even if
     * <code>-SNAPSHOT</code> dependencies exists in the pom.
     *
     * @since 1.2
     */
    @Parameter(property = "allowSnapshots", defaultValue = "false", required = false)
    private Boolean allowSnapshots;

    /**
     * If the project has a parent with a release version it will be replaced
     * with the next <code>-SNAPSHOT</code> version (if it has been deployed).
     * This action is performed on the develop branch after the merge.
     *
     * @since 1.2
     */
    @Parameter(property = "updateParent", defaultValue = "false", required = false)
    private Boolean updateParent;

    /**
     * On the release branch before finish is called any dependencies with a
     * <code>-SNAPSHOT</code> version are replaced with the corresponding
     * release version (if it has been released).
     *
     * On the develop branch after the merge any dependencies with a release
     * version are replaced with the next <code>-SNAPSHOT</code> version (if it
     * has been deployed).
     *
     * @since 1.2
     */
    @Parameter(property = "updateDependencies", defaultValue = "false", required = false)
    private Boolean updateDependencies;

    /**
     * If <code>updateDependencies</code> is set, then this should contain a
     * comma separated list of artifact patterns to include. Follows the pattern <code>groupId:artifactId:type:classifier:version<code>
     *
     * @since 1.5
     */
    @Parameter(property = "includes", defaultValue = "", required = false)
    private String includes;

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
     * released artifact, if appropriate. If set to true, the release-finish
     * will set the property "performRelease" to true, which activates the
     * profile "release-profile", which is inherited from the super pom.
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
     * If <code>true</code>, the local release branch will not be deleted after
     * the merge.
     *
     * @since 1.6
     */
    @Parameter(property = "keep", defaultValue = "false", required = false)
    private Boolean keepLocal;

    /**
     * If <code>true</code>, the remote release branch will not be deleted after
     * the merge.
     *
     * @since 1.6
     */
    @Parameter(property = "keep", defaultValue = "true", required = false)
    private Boolean keepRemote;

    /**
     * If <code>true</code>, the release tag will be signed.
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
        getLog().debug("Finishing release");

        /* Fetch any new tags and prune any branches that may already be deleted */
        getGitflowInit().executeRemote("git fetch --tags --prune");

        /* Get release branch name */
        String prefix = getReleaseBranchPrefix();
        List<String> releaseBranches = getGitflowInit().gitLocalReleaseBranches();
        if (releaseBranches.isEmpty()) {
            throw new MojoFailureException("Could not find any local release branch!");
        }

        if (StringUtils.isBlank(releaseName)) {
            if (releaseBranches.size() == 1) {
                String releaseBranch = releaseBranches.get(0);
                releaseName = trimReleaseName(releaseBranch);
            } else {
                String releaseBranch = promptForExistingReleaseBranch(prefix, releaseBranches);
                releaseName = trimReleaseName(releaseBranch);
            }

        } else {
            releaseName = trimReleaseName(releaseName);
            if (!getGitflowInit().gitLocalBranchExists(prefix + releaseName)) {
                throw new MojoFailureException("No local release branch named '" + prefix + releaseName + "' exists!");
            }
        }

        getLog().info("Finishing release '" + releaseName + "'");
        
        String releaseBranch = prefix + releaseName;

        /* Switch to develop branch and get its current version */
        String developBranch = getGitflowInit().getDevelopBranch();
        getGitflowInit().executeLocal("git checkout " + developBranch);
        reloadReactorProjects();
        String developVersion = project.getVersion();
        getLog().debug("develop version = " + developVersion);

        /* Switch to release branch and set poms to release version */
        getGitflowInit().executeLocal("git checkout " + releaseBranch);
        reloadReactorProjects();
        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(project.getVersion());
        String releaseVersion;
        if ("SNAPSHOT".equals(artifactVersion.getBuildSpecifier())) {
            releaseVersion = getReleaseVersion(project.getVersion());
        } else {
            releaseVersion = project.getVersion();
        }
        getLog().debug("release version = " + releaseVersion);

        boolean setVersion = setVersion(releaseVersion, pushReleaseFinish, releaseBranch);

        /* Update release branch dependencies to release version */
        boolean setNextVersions = false;
        if (updateDependencies) {
            reloadReactorProjects();
            setNextVersions = setNextVersions(false, updateParent, includes);
        }

        if (!allowSnapshots) {
            reloadReactorProjects();
            try {
                checkForSnapshotDependencies();
            } catch (MojoExecutionException mee) {
                // reset setNextVersions and/or setVersion commits to allow user fix & push SNAPSHOT dependencies
                // but can only reset if it the commits have not been pushed */
                if (!pushReleaseFinish) {
                    if (setNextVersions) {
                        getGitflowInit().executeLocal("git reset --hard HEAD~1");
                    }
                    if (setVersion) {
                        getGitflowInit().executeLocal("git reset --hard HEAD~1");
                    }
                }
                throw mee;
            }
        }

        /* finish release */
        GitflowRelease gitflowRelease = new GitflowRelease();
        gitflowRelease.setInit(getGitflowInit());
        gitflowRelease.setMsgPrefix(getMsgPrefix());
        gitflowRelease.setMsgSuffix(getMsgSuffix());
        gitflowRelease.setPush(pushReleaseBranch);
        gitflowRelease.setSquash(squash);
        gitflowRelease.setKeepLocal(keepLocal);
        gitflowRelease.setKeepRemote(keepRemote);
        gitflowRelease.setSign(sign);
        gitflowRelease.setSigningkey(signingkey);

        /* 1. merge to master */
        try {
            gitflowRelease.finishToMaster(releaseBranch, pushReleaseFinish);
        } catch (GitflowException ge) {
            throw new MojoFailureException(ge.getMessage());
        } catch (GitflowMergeConflictException gmce) {
            throw new MojoFailureException(gmce.getMessage());
        }

        /* 2. make versions in release and develop branches match to avoid conflicts */
        getGitflowInit().executeLocal("git checkout " + releaseBranch);
        reloadReactorProjects();
        boolean setDevVersion = setVersion(developVersion, pushReleaseFinish, releaseBranch);

        /* 3. merge to develop */
        try {
            gitflowRelease.finishToDevelop(releaseBranch, pushReleaseFinish);
        } catch (GitflowException ge) {
            // reset setVersion commit and allow user fix whatever exception occurred
            // but can only reset if the commit has not been pushed */
            if (!pushReleaseFinish && setDevVersion) {
                getGitflowInit().executeLocal("git reset --hard HEAD~1");
            }
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

        /* Update develop branch dependencies to next snapshot version (if deployed) */
        if (updateDependencies) {
            reloadReactorProjects();
            setNextVersions(true, updateParent, includes);
        }

        /* Switch to release tag and deploy it */
        getGitflowInit().executeLocal("git checkout " + getGitflowInit().getVersionTagPrefix() + releaseVersion);
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
            getLog().debug("Skipping both install and deploy for release tag " + releaseBranch);
        }

        getGitflowInit().executeLocal("git checkout " + developBranch);
    }

    private String promptForExistingReleaseBranch(String prefix, List<String> releaseBranches) throws MojoFailureException {
        List<String> choices = releaseBranches;

        /* if current branch is a feature branch at it to start of list so it is the default in prompt */
        String currentBranch = getGitflowInit().gitCurrentBranch();
        if (currentBranch.startsWith(prefix)) {
            choices = rearrange(currentBranch, releaseBranches);
        }

        String name = "";
        try {
            prompter.promptChoice("Release branches", "Please select a release branch to finish", choices);
        } catch (IOException ex) {
            throw new MojoFailureException("Error reading release name from command line " + ex.getMessage());
        }
        return name.trim();
    }
}
