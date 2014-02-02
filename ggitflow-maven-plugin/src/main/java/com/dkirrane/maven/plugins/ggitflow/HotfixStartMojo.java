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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.jfrog.hudson.util.GenericArtifactVersion;
import static org.jfrog.hudson.util.GenericArtifactVersion.DEFAULT_VERSION_COMPONENT_SEPARATOR;
import static org.jfrog.hudson.util.GenericArtifactVersion.SNAPSHOT_QUALIFIER;

/**
 *
 */
@Mojo(name = "hotfix-start", aggregator = true, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class HotfixStartMojo extends HotfixAbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        getGitflowInit().executeLocal("git checkout " + getGitflowInit().getMasterBrnName());
        
        String currentVersion = project.getVersion();
        String hotfixVersion = getHotfixVersion(currentVersion);
        String hotfixSnapshotVersion = getHotfixSnapshotVersion(currentVersion);

        getLog().info("Starting hotfix '" + hotfixVersion + "'");
        getLog().debug("msgPrefix '" + getMsgPrefix() + "'");
        getLog().debug("msgSuffix '" + getMsgSuffix() + "'");

        GitflowHotfix gitflowHotfix = new GitflowHotfix();
        gitflowHotfix.setInit(getGitflowInit());
        gitflowHotfix.setMsgPrefix(getMsgPrefix());
        gitflowHotfix.setMsgSuffix(getMsgSuffix());

        try {
            gitflowHotfix.start(hotfixVersion);
        } catch (GitflowException ge) {
            throw new MojoFailureException(ge.getMessage());
        }

        setVersion(hotfixSnapshotVersion);

        String prefix = getGitflowInit().getHotfixBranchPrefix();
        if (getGitflowInit().gitRemoteBranchExists(prefix + hotfixVersion)) {
            getGitflowInit().executeRemote("git push " + getGitflowInit().getOrigin() + " " + prefix + hotfixVersion);
        }
    }

    private String getHotfixVersion(String currentVersion) throws MojoFailureException {
        getLog().info("getHotfixVersion from '" + currentVersion + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(currentVersion);

        final StringBuilder result = new StringBuilder(30);
        if (artifactVersion.getPrimaryNumberCount() < 3) {
            String primaryNumbersAsString = artifactVersion.getPrimaryNumbersAsString();
            String annotationAsString = artifactVersion.getAnnotationAsString();
            result.append(primaryNumbersAsString).append('.').append('0');
            result.append(annotationAsString);
        } else {
            artifactVersion.upgradeAnnotationRevision();
            result.append(artifactVersion.toString());
        }
        return result.toString();
    }

    private String getHotfixSnapshotVersion(String currentVersion) throws MojoFailureException {
        getLog().info("getHotfixSnapshotVersion from '" + currentVersion + "'");

        final StringBuilder result = new StringBuilder(30);

        String hotfixVersion = getHotfixVersion(currentVersion);
        result.append(hotfixVersion);

        result.append(DEFAULT_VERSION_COMPONENT_SEPARATOR).append(SNAPSHOT_QUALIFIER);

        return result.toString();
    }
}
