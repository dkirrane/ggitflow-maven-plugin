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

import com.dkirrane.gitflow.groovy.GitflowSupport;
import com.dkirrane.gitflow.groovy.ex.GitflowException;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.jfrog.hudson.util.GenericArtifactVersion;
import static org.jfrog.hudson.util.GenericArtifactVersion.DEFAULT_VERSION_COMPONENT_SEPARATOR;
import static org.jfrog.hudson.util.GenericArtifactVersion.SNAPSHOT_QUALIFIER;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a new support branch from a specific commit on the master branch.
 */
@Mojo(name = "support-start", aggregator = true, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SupportStartMojo extends AbstractGitflowMojo {

    private static final Logger LOG = LoggerFactory.getLogger(SupportStartMojo.class.getName());

    /**
     * The commit to start the support branch from.
     *
     * @since 1.2
     */
    @Parameter(property = "startCommit", defaultValue = "", required = false)
    protected String startCommit;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        String prefix = getGitflowInit().getSupportBranchPrefix();

        List<String> tags = getGitflowInit().gitAllTags();
        if (tags.isEmpty()) {
            throw new MojoFailureException("Could not find any tags to create support branch from!");
        }

        String baseName = promptForExistingTagName(tags, tags.get(tags.size() - 1));

        getGitflowInit().executeLocal("git checkout " + baseName);
        reloadReactorProjects();
        String supportVersion = getSupportVersion(project.getVersion());
        String supportSnapshotVersion = getSupportSnapshotVersion(project.getVersion());

        LOG.info("Starting support branch '" + supportVersion + "'");
        LOG.debug("msgPrefix '" + getMsgPrefix() + "'");
        LOG.debug("msgSuffix '" + getMsgSuffix() + "'");

        GitflowSupport gitflowSupport = new GitflowSupport();
        gitflowSupport.setInit(getGitflowInit());
        gitflowSupport.setMsgPrefix(getMsgPrefix());
        gitflowSupport.setMsgSuffix(getMsgSuffix());
        gitflowSupport.setStartCommit(startCommit);

        try {
            gitflowSupport.start(supportVersion);
        } catch (GitflowException ge) {
            throw new MojoFailureException(ge.getMessage());
        }

        setVersion(supportSnapshotVersion);

        if (getGitflowInit().gitRemoteBranchExists(prefix + supportVersion)) {
            getGitflowInit().executeRemote("git push " + getGitflowInit().getOrigin() + " " + prefix + supportVersion);
        }
    }

    private String promptForExistingTagName(List<String> branches, String defaultBrnName) throws MojoFailureException {
        String message = "Create a support branch from tag:";

        String name = "";
        try {
            name = prompter.prompt(message, branches, defaultBrnName);
        } catch (PrompterException e) {
            throw new MojoFailureException("Error reading selected Tag name from command line " + e.getMessage());
        }

        return name;
    }

    private String getSupportVersion(String currentVersion) throws MojoFailureException {
        LOG.debug("getSupportVersion from '" + currentVersion + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(currentVersion);

        StringBuilder sb = new StringBuilder(10);
        int pCount = artifactVersion.getPrimaryNumberCount();
        if (pCount < 3) {
            // Support versions should be like 1.0.x e.g. 1.0.1, 1.0.2 etc
            sb.append(artifactVersion.getPrimaryNumbersAsString()).append('.').append('x');
            sb.append(artifactVersion.getAnnotationAsString()).append(artifactVersion.getBuildSpecifierAsString());
        } else {
            // Support versions should be like 1.0.0-xx e.g. 1.0.0-01, 1.0.0-02 etc
            if (null != artifactVersion.getAnnotation()) {
                throw new MojoFailureException("Cannot start Support branch. Primary number " + artifactVersion.getPrimaryNumbersAsString() + " and annotations are already set " + artifactVersion.getAnnotation());
            }
            sb.append(artifactVersion.getPrimaryNumbersAsString()).append('-').append("xx");
        }

        return sb.toString();
    }

    private String getSupportSnapshotVersion(String currentVersion) throws MojoFailureException {
        LOG.debug("getSupportSnapshotVersion from '" + currentVersion + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(currentVersion);        

        StringBuilder sb = new StringBuilder(10);
        int pCount = artifactVersion.getPrimaryNumberCount();
        if (pCount < 3) {
            // Support versions should be like 1.0.x e.g. 1.0.1, 1.0.2 etc
            sb.append(artifactVersion.getPrimaryNumbersAsString()).append('.').append('1');
            sb.append(artifactVersion.getAnnotationAsString());
        } else {
            // Support versions should be like 1.0.0-xx e.g. 1.0.0-01, 1.0.0-02 etc
            if (null != artifactVersion.getAnnotation()) {
                throw new MojoFailureException("Cannot start Support branch. Primary number " + artifactVersion.getPrimaryNumbersAsString() + " and annotations are already set " + artifactVersion.getAnnotation());
            }
            sb.append(artifactVersion.getPrimaryNumbersAsString()).append('-').append("01");
        }        

        sb.append(DEFAULT_VERSION_COMPONENT_SEPARATOR).append(SNAPSHOT_QUALIFIER);

        return sb.toString();
    }
}
