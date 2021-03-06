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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.hudson.util.GenericArtifactVersion;

/**
 * Creates a tag on a support branch when releasing of a support branch. Note:
 * this does not merge support branch changes back to develop
 */
@Mojo(name = "support-tag", aggregator = true)
public class SupportTagMojo extends AbstractSupportMojo {

    /**
     * If <code>true</code>, the tag can still get created even if
     * <code>-SNAPSHOT</code> dependencies exists in the pom.
     *
     * @since 2.3
     */
    @Parameter(property = "allowSnapshots", defaultValue = "false", required = false)
    private Boolean allowSnapshots;

    /**
     * The message to append add to the release tag
     *
     * @since 1.2
     */
    @Parameter(property = "tagMsg", defaultValue = "", required = false)
    private String tagMsg;

    /**
     * If <code>true</code>, the support tag will be signed.
     *
     * @since 2.3
     */
    @Parameter(property = "sign", defaultValue = "false", required = false)
    private Boolean sign;

    /**
     * The GNU Privacy Guard (GPG) private key used to sign the tag.
     *
     * @since 2.3
     */
    @Parameter(property = "signingkey", defaultValue = "", required = false)
    private String signingkey;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().debug("Tagging support branch");

        /* Get support branch name */
        String prefix = getSupportBranchPrefix();
        List<String> supportBranches = getGitflowInit().gitLocalSupportBranches();
        if (supportBranches.isEmpty()) {
            exceptionMapper.handle(new MojoFailureException("Could not find any local support branch!"));
        }

        if (StringUtils.isBlank(supportName)) {
            if (supportBranches.size() == 1) {
                String supportBranch = supportBranches.get(0);
                supportName = trimSupportName(supportBranch);
            } else {
                String supportBranch = promptForExistingSupportBranch(prefix, supportBranches);
                supportName = trimSupportName(supportBranch);
            }

        } else {
            supportName = trimSupportName(supportName);
            if (!getGitflowInit().gitLocalBranchExists(prefix + supportName)) {
                String msg = "No local support branch named '" + prefix + supportName + "' exists!";
                exceptionMapper.handle(new MojoFailureException(msg));
            }
        }

        getLog().info("Tagging support branch '" + supportName + "'");

        String supportBranch = prefix + supportName;

        /* Switch to support branch and get its current version */
        getGitflowInit().executeLocal("git checkout " + supportBranch);
        reloadReactorProjects();
        String snapshotVersion = project.getVersion();

        /* Get release version for support tag */
        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(snapshotVersion);
        String supportVersion;
        if ("SNAPSHOT".equals(artifactVersion.getBuildSpecifier())) {
            supportVersion = getReleaseVersion(project.getVersion());
        } else {
            supportVersion = project.getVersion();
        }
        getLog().debug("support version = " + supportVersion);

        getGitflowInit().requireTagAbsent(supportVersion);

        /* Set support branch to release version */
        boolean setVersion = setVersion(supportVersion, supportBranch, false);

        if (!allowSnapshots) {
            reloadReactorProjects();
            try {
                checkForSnapshotDependencies();
            } catch (MojoExecutionException mee) {
                // reset setVersion commit to allow user fix & push SNAPSHOT dependencies
                if (setVersion) {
                    getGitflowInit().executeLocal("git reset --hard HEAD~1");
                }
                exceptionMapper.handle(mee);
            }
        }

        /* tag support branch */
        tagMsg = StringUtils.isBlank(tagMsg) ? "" : " " + tagMsg;
        String tagMessage = "Support release version " + supportVersion + tagMsg ;
        String tagName = supportVersion;
        String tagString = getGitflowInit().executeLocal("git tag -a -m \"" + tagMessage + "\" " + tagName + " " + supportBranch);
        getLog().debug("Git tag output = " + tagString);

        /* Increment support branch to next version */
        String nextSupportVersion = getNextSupportVersion(snapshotVersion);
        setVersion(nextSupportVersion, supportBranch, false);
        promptToPushSupportBranchAndTag(supportBranch, tagName);

    }

    private String promptForExistingSupportBranch(String prefix, List<String> supportBranches) throws MojoFailureException {
        List<String> choices = supportBranches;

        /* if current branch is a support branch at it to start of list so it is the default in prompt */
        String currentBranch = getGitflowInit().gitCurrentBranch();
        if (currentBranch.startsWith(prefix)) {
            choices = rearrange(currentBranch, supportBranches);
        }

        String name = "";
        try {
            name = prompter.promptChoice("Support branches", "Please select a support branch to tag", choices);
        } catch (IOException ex) {
            throw new MojoFailureException("Error reading release name from command line " + ex.getMessage());
        }
        return name.trim();
    }

    private void promptToPushSupportBranchAndTag(String supportBranch, String supportTag) throws MojoFailureException {
        String origin = getGitflowInit().getOrigin();

        boolean yes = false;
        if (session.getRequest().isInteractiveMode()) {
            prompter.pushPrompt("Are you ready to push?", Arrays.asList(supportTag), Arrays.asList(supportBranch), Collections.EMPTY_LIST);
            try {
                yes = prompter.promptYesNo("Do you want to continue");
            } catch (IOException e) {
                throw new MojoFailureException("Error reading user input from command line " + e.getMessage());
            }
        } else {
            yes = true; // push in maven --batch mode
        }

        if (yes) {
            if (getGitflowInit().gitRemoteBranchExists(supportBranch)) {
                getLog().info("Pushing tag " + supportTag);
                getGitflowInit().executeRemote("git push " + origin + " " + supportTag);
                getLog().info("Pushing " + supportBranch);
                getGitflowInit().executeRemote("git push " + origin + " " + supportBranch);
            } else {
                getLog().info("");
                getLog().warn("===> Branch '" + supportBranch + "' does not exist on '" + origin + "'");
                getLog().warn("===> Once ready you MUST manually push '" + supportBranch + "' branch and support tag '" + supportTag + "' to '" + origin + "'");
                getLog().info("");
            }
        } else {
            getLog().info("");
            getLog().warn("===> Once ready you MUST manually push '" + supportBranch + "' branch and support tag '" + supportTag + "' to '" + origin + "'");
            getLog().info("");
        }
    }
}
