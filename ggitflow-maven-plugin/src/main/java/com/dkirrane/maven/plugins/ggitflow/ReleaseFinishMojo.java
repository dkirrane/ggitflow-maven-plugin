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
import com.dkirrane.gitflow.groovy.ex.GitCommandException;
import com.dkirrane.gitflow.groovy.ex.GitflowException;
import com.dkirrane.gitflow.groovy.ex.GitflowMergeConflictException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.hudson.util.GenericArtifactVersion;

/**
 * Merges a release branch back into the develop and master branch and then
 * creates a tag for the release on master.
 */
@Mojo(name = "release-finish", aggregator = true)
public class ReleaseFinishMojo extends AbstractReleaseMojo {

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
     * If <code>true</code>, the release can still finish even if
     * <code>-SNAPSHOT</code> dependencies exists in the pom.
     *
     * @since 1.2
     */
    @Parameter(property = "allowSnapshots", defaultValue = "false", required = false)
    private Boolean allowSnapshots;

    /**
     * If <code>true</code>, all commits to the branch will be squashed into a
     * single commit before the merge.
     *
     * @since 1.2
     */
    @Parameter(property = "squash", defaultValue = "false", required = false)
    private Boolean squash;

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

        /* Get release branch name */
        String prefix = getReleaseBranchPrefix();
        List<String> releaseBranches = getGitflowInit().gitLocalReleaseBranches();
        if (releaseBranches.isEmpty()) {
            exceptionMapper.handle(new MojoFailureException("Could not find any local release branch!"));
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
                exceptionMapper.handle(new MojoFailureException("No local release branch named '" + prefix + releaseName + "' exists!"));
            }
        }

        getLog().info("Finishing release '" + releaseName + "'");

        String releaseBranch = prefix + releaseName;
        String tagName = getVersionTagPrefix() + releaseName;
        String developBranch = getGitflowInit().getDevelopBranch();
        String masterBranch = getGitflowInit().getMasterBranch();
        String origin = getGitflowInit().getOrigin();

        GitflowRelease gitflowRelease = new GitflowRelease();
        gitflowRelease.setInit(getGitflowInit());
        gitflowRelease.setMsgPrefix(getMsgPrefix());
        gitflowRelease.setMsgSuffix(getMsgSuffix());
        gitflowRelease.setPush(false);
        gitflowRelease.setSquash(squash);
        gitflowRelease.setSign(sign);
        gitflowRelease.setSigningkey(signingkey);

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

        /* If tag exists we skip merge to master as merge already took place. Possible re-run after merge conflict */
        if (!getGitflowInit().gitTagExists(tagName)) { // @TODO and should also check that last release branch commit is on master

            /* Before setting release version check if release branch was already merged to master */
            boolean setVersion = setVersion(releaseVersion, releaseBranch, false); // don't push so can can reset if needed

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
                    if (setNextVersions) {
                        getGitflowInit().executeLocal("git reset --hard HEAD~1");
                    }
                    if (setVersion) {
                        getGitflowInit().executeLocal("git reset --hard HEAD~1");
                    }
                    exceptionMapper.handle(mee);
                }
            }

            /* 1. merge to master */
            try {
                gitflowRelease.finishToMaster(releaseBranch, tagName);
            } catch (GitCommandException gce) {
                String header = "Error merging branch '" + releaseBranch + "' into '" + masterBranch + "'";
                exceptionMapper.handle(header, gce);
            } catch (GitflowException ge) {
                String header = "Error merging branch '" + releaseBranch + "' into '" + masterBranch + "'";
                exceptionMapper.handle(header, ge);
            } catch (GitflowMergeConflictException gmce) {
                String header = "Merge conflict merging branch '" + releaseBranch + "' into '" + masterBranch + "'";
                exceptionMapper.handle(header, gmce);
            }
        } else {
            getLog().warn("Tag " + tagName + " already exists. Skipping merge of release branch '" + releaseBranch + "' into '" + masterBranch + "'");
        }

        /* 2. make versions in release and develop branches match to avoid conflicts */
        getGitflowInit().executeLocal("git checkout " + developBranch);
        reloadReactorProjects();
        String developVersion = project.getVersion();
        getLog().debug("develop version = " + developVersion);
        getGitflowInit().executeLocal("git checkout " + releaseBranch);
        reloadReactorProjects();
        boolean setDevVersion = setVersion(developVersion, releaseBranch, false); // don't push so can can reset if needed

        /* 3. merge to develop */
        try {
            gitflowRelease.finishToDevelop(releaseBranch, tagName);
        } catch (GitCommandException gce) {
            // reset setVersion commit and allow user fix whatever exception occurred
            // but can only reset if the commit has not been pushed
            if (setDevVersion) {
                getGitflowInit().executeLocal("git reset --hard HEAD~1");
            }
            String header = "Error merging branch '" + releaseBranch + "' into '" + developBranch + "'";
            exceptionMapper.handle(header, gce);
        } catch (GitflowException ge) {
            // reset setVersion commit and allow user fix whatever exception occurred
            // but can only reset if the commit has not been pushed
            if (setDevVersion) {
                getGitflowInit().executeLocal("git reset --hard HEAD~1");
            }
            String header = "Error merging branch '" + releaseBranch + "' into '" + developBranch + "'";
            exceptionMapper.handle(header, ge);
        } catch (GitflowMergeConflictException gmce) {
            String header = "Merge conflict merging branch '" + releaseBranch + "' into '" + developBranch + "'";
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
                prompter.pushPrompt("Are you ready to push?", Arrays.asList(tagName, masterBranch, developBranch), Arrays.asList(releaseBranch, origin + '/' + releaseBranch));
                boolean yes;
                try {
                    yes = prompter.promptYesNo("Do you want to continue");
                } catch (IOException e) {
                    throw new MojoFailureException("Error reading user input from command line " + e.getMessage());
                }

                if (yes) {
                    gitflowRelease.publish(releaseBranch, tagName, true);
                } else {
                    gitflowRelease.publish(releaseBranch, tagName, false);
                }
            } else {
                gitflowRelease.publish(releaseBranch, tagName, true);
            }
        } catch (GitCommandException gce) {
            String header = "Failed to push release finish";
            exceptionMapper.handle(header, gce);
        } catch (GitflowException ge) {
            String header = "Failed to push release finish";
            exceptionMapper.handle(header, ge);
        }

        /* Update develop branch dependencies to next snapshot version (if deployed) */
        if (updateDependencies) {
            reloadReactorProjects();
            setNextVersions(true, updateParent, includes);
        }

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
