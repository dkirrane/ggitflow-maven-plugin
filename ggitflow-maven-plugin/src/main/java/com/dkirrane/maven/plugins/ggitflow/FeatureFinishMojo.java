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
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Merges a feature branch back into the develop.
 */
@Mojo(name = "feature-finish", aggregator = true)
public class FeatureFinishMojo extends AbstractFeatureMojo {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureFinishMojo.class.getName());

    /**
     * If true, the feature branch rebases onto develop, then finish can
     * fast-forward merge the feature branch back into develop.
     *
     * @since 1.2
     */
    @Parameter(defaultValue = "false", property = "isRebase")
    private boolean isRebase = false;

    /**
     * If true, and isRebase parameter is also true, then an interactive rebase
     * is performed for the feature branch.
     *
     * @since 1.2
     */
    @Parameter(defaultValue = "false", property = "isInteractive")
    private boolean isInteractive = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        LOG.debug("Finishing feature");
        
        List<String> featureBranches = getGitflowInit().gitLocalFeatureBranches();

        if (null == featureBranches || featureBranches.isEmpty()) {
            throw new MojoFailureException("No local feature branches exit!");            
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
        }

        featureName = promptForExistingFeatureName(featureBranches, featureName);
        
        LOG.info("Finishing feature '{}'", featureName);

        if (enableFeatureVersions) {
            /* Switch to develop branch and get its current version */
            getGitflowInit().executeLocal("git checkout " + getGitflowInit().getDevelopBranch());
            reloadReactorProjects();
            String developVersion = project.getVersion();
            LOG.debug("develop version = " + developVersion);

            /* Switch to feature branch and get its current version */
            getGitflowInit().executeLocal("git checkout " + featureName);
            reloadReactorProjects();
            String featureVersion = project.getVersion();
            LOG.debug("feature version = " + featureVersion);

            setVersion(developVersion);
        }

        if (!skipBuild) {
            clean();
            install();
        } else {
            LOG.debug("Skipping both install and deploy");
        }

        GitflowFeature gitflowFeature = new GitflowFeature();
        gitflowFeature.setInit(getGitflowInit());
        gitflowFeature.setMsgPrefix(getMsgPrefix());
        gitflowFeature.setMsgSuffix(getMsgSuffix());
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
