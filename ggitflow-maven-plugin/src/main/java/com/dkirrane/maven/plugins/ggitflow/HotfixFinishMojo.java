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
 * Merges a hotfix branch back into the develop and master branch and then
 * creates a tag for the hotfix on master.
 */
@Mojo(name = "hotfix-finish", aggregator = true)
public class HotfixFinishMojo extends AbstractHotfixMojo {

    protected String hotfixName;

    /**
     * If <code>true</code>, the hotfix can still finish even if
     * <code>-SNAPSHOT</code> dependencies exists in the pom.
     *
     * @since 1.2
     */
    @Parameter(property = "allowSnapshots", defaultValue = "false", required = false)
    private boolean allowSnapshots;

    /**
     * If <code>true</code>, all commits to the branch will be squashed into a
     * single commit before the merge.
     *
     * @since 1.2
     */
    @Parameter(property = "squash", defaultValue = "false", required = false)
    private Boolean squash;

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
        String tagName = getVersionTagPrefix() + hotfixName;
        String developBranch = getGitflowInit().getDevelopBranch();
        String masterBranch = getGitflowInit().getMasterBranch();
        String origin = getGitflowInit().getOrigin();

        GitflowHotfix gitflowHotfix = new GitflowHotfix();
        gitflowHotfix.setInit(getGitflowInit());
        gitflowHotfix.setMsgPrefix(getMsgPrefix());
        gitflowHotfix.setMsgSuffix(getMsgSuffix());
        gitflowHotfix.setPush(false);
        gitflowHotfix.setSquash(squash);
        gitflowHotfix.setSign(sign);
        gitflowHotfix.setSigningkey(signingkey);

        /* Switch to hotfix branch and set poms to hotfix version */
        getGitflowInit().executeLocal("git checkout " + hotfixBranch);
        reloadReactorProjects();
        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(project.getVersion());
        String hotfixVersion;
        if ("SNAPSHOT".equals(artifactVersion.getBuildSpecifier())) {
            hotfixVersion = getReleaseVersion(project.getVersion());
        } else {
            hotfixVersion = project.getVersion();
        }
        getLog().debug("hotfix version = " + hotfixVersion);

        /* If tag exists we skip merge to master as merge already took place. Possible re-run after merge conflict */
        if (!getGitflowInit().gitTagExists(tagName)) { // @TODO and should also check that last hotfix branch commit is on master

            /* Before setting hotfix version check if hotfix branch was already merged to master */
            boolean setVersion = setVersion(hotfixVersion, hotfixBranch, false); // don't push so can can reset if needed

            if (!allowSnapshots) {
                reloadReactorProjects();
                try {
                    checkForSnapshotDependencies();
                } catch (MojoExecutionException mee) {
                    // reset setVersion commits to allow user fix & push SNAPSHOT dependencies
                    // but can only reset if it the commits have not been pushed */
                    if (setVersion) {
                        getGitflowInit().executeLocal("git reset --hard HEAD~1");
                    }
                    exceptionMapper.handle(mee);
                }
            }

            /* 1. merge to master */
            try {
                gitflowHotfix.finishToMaster(hotfixBranch, tagName);
            } catch (GitCommandException gce) {
                String header = "Error merging branch '" + hotfixBranch + "' into '" + masterBranch + "'";
                exceptionMapper.handle(header, gce);
            } catch (GitflowException ge) {
                String header = "Error merging branch '" + hotfixBranch + "' into '" + masterBranch + "'";
                exceptionMapper.handle(header, ge);
            } catch (GitflowMergeConflictException gmce) {
                String header = "Merge conflict merging branch '" + hotfixBranch + "' into '" + masterBranch + "'";
                exceptionMapper.handle(header, gmce);
            }
        } else {
            getLog().warn("Tag " + tagName + " already exists. Skipping merge of hotfix branch '" + hotfixBranch + "' into '" + masterBranch + "'");
        }

        /* 2. make versions in hotfix and develop branches match to avoid conflicts */
        getGitflowInit().executeLocal("git checkout " + developBranch);
        reloadReactorProjects();
        String developVersion = project.getVersion();
        getLog().debug("develop version = " + developVersion);
        getGitflowInit().executeLocal("git checkout " + hotfixBranch);
        reloadReactorProjects();
        boolean setDevVersion = setVersion(developVersion, hotfixBranch, false); // don't push so can can reset if needed

        /* 3. merge to develop */
        try {
            gitflowHotfix.finishToDevelop(hotfixBranch, tagName);
        } catch (GitCommandException gce) {
            // reset setVersion commit and allow user fix whatever exception occurred
            // but can only reset if the commit has not been pushed
            if (setDevVersion) {
                getGitflowInit().executeLocal("git reset --hard HEAD~1");
            }
            String header = "Error merging branch '" + hotfixBranch + "' into '" + developBranch + "'";
            exceptionMapper.handle(header, gce);
        } catch (GitflowException ge) {
            // reset setVersion commit and allow user fix whatever exception occurred
            // but can only reset if the commit has not been pushed
            if (setDevVersion) {
                getGitflowInit().executeLocal("git reset --hard HEAD~1");
            }
            String header = "Error merging branch '" + hotfixBranch + "' into '" + developBranch + "'";
            exceptionMapper.handle(header, ge);
        } catch (GitflowMergeConflictException gmce) {
            String header = "Merge conflict merging branch '" + hotfixBranch + "' into '" + developBranch + "'";
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
                prompter.pushPrompt("Are you ready to push?", Arrays.asList(tagName, masterBranch, developBranch), Arrays.asList(hotfixBranch, origin + '/' + hotfixBranch));
                boolean yes;
                try {
                    yes = prompter.promptYesNo("Do you want to continue");
                } catch (IOException e) {
                    throw new MojoFailureException("Error reading user input from command line " + e.getMessage());
                }

                if (yes) {
                    gitflowHotfix.publish(hotfixBranch, tagName, true);
                } else {
                    gitflowHotfix.publish(hotfixBranch, tagName, false);
                }
            } else {
                gitflowHotfix.publish(hotfixBranch, tagName, true);
            }
        } catch (GitCommandException gce) {
            String header = "Failed to push hotfix finish";
            exceptionMapper.handle(header, gce);
        } catch (GitflowException ge) {
            String header = "Failed to push hotfix finish";
            exceptionMapper.handle(header, ge);
        }
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
