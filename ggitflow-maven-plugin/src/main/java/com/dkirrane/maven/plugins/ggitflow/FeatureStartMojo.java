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
import com.dkirrane.maven.plugins.ggitflow.util.MavenUtil;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a new feature branch off of the develop branch.
 */
@Mojo(name = "feature-start", aggregator = true, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class FeatureStartMojo extends AbstractFeatureMojo {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureStartMojo.class.getName());

    /**
     * The commit to start the feature branch from.
     *
     * @since 1.2
     */
    @Parameter(property = "startCommit", defaultValue = "", required = false)
    protected String startCommit;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        String prefix = getFeatureBranchPrefix();
        if (StringUtils.isBlank(featureName)) {
            String message = "What is the feature branch name? " + prefix;
            try {
                featureName = prompter.prompt(message);
                if (StringUtils.isBlank(featureName)) {
                    throw new MojoFailureException("Parameter <featureName> cannot be null or empty.");
                }
            } catch (PrompterException ex) {
                throw new MojoExecutionException("Error reading feature name from command line " + ex.getMessage(), ex);
            }
        }
        featureName = getFeatureName(featureName);

        LOG.info("Starting feature '" + featureName + "'");
        LOG.debug("msgPrefix '" + getMsgPrefix() + "'");
        LOG.debug("msgSuffix '" + getMsgSuffix() + "'");

        GitflowFeature gitflowFeature = new GitflowFeature();
        gitflowFeature.setInit(getGitflowInit());
        gitflowFeature.setMsgPrefix(getMsgPrefix());
        gitflowFeature.setMsgSuffix(getMsgSuffix());
        gitflowFeature.setPush(pushFeatures);
        gitflowFeature.setStartCommit(startCommit);

        try {
            gitflowFeature.start(featureName);
        } catch (GitflowException ge) {
            throw new MojoFailureException(ge.getMessage());
        }

        if (enableFeatureVersions) {
            /* Already on feature branch so just get its current version */
            Model model = MavenUtil.readPom(reactorProjects);
            String currentVersion = model.getVersion();

            String featureVersion = getFeatureVersion(currentVersion, featureName);
            setVersion(featureVersion);

            if (getGitflowInit().gitRemoteBranchExists(prefix + featureName)) {
                getGitflowInit().executeRemote("git push " + getGitflowInit().getOrigin() + " " + prefix + featureName);
            }

            /* print feature version */
            reloadReactorProjects();
            LOG.debug("project = " + project.getVersion());
        }
    }

    public String getFeatureName() {
        return featureName;
    }

}
