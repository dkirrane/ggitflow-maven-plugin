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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.jfrog.hudson.util.GenericArtifactVersion;

import static com.dkirrane.gitflow.groovy.Constants.*;

import com.dkirrane.gitflow.groovy.GitflowInit;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 */
public class AbstractReleaseMojo extends AbstractGitflowMojo {

    @Parameter(property = "releaseName")
    protected String releaseName;

    @Parameter(defaultValue = "false", property = "pushReleases")
    private boolean pushReleases = false;

    private GitflowInit init;

    public GitflowInit getGitflowInit() {
        if (null == init) {
            init = new GitflowInit();
            init.setRepoDir(getProject().getBasedir());
        }
        return init;
    }

    public String getReleaseBranchPrefix() {
        String prefix = getGitflowInit().getReleaseBranchPrefix();
        if (StringUtils.isBlank(prefix)) {
            prefix = DEFAULT_RELEASE_BRN_PREFIX;
        }
        return prefix;
    }

    public String getReleaseVersion(String version) throws MojoFailureException {
        getLog().info("Project version '" + version + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(version);

        String primaryNumbersAsString = artifactVersion.getPrimaryNumbersAsString();
        String annotationAsString = artifactVersion.getAnnotationAsString();
        String buildSpecifierAsString = artifactVersion.getBuildSpecifierAsString();

        final StringBuilder result = new StringBuilder(30);
        result.append(primaryNumbersAsString).append(annotationAsString);

        if (!StringUtils.isBlank(buildSpecifierAsString)) {
            getLog().warn("Removing build specifier " + buildSpecifierAsString + " from version " + version);
        }

        return result.toString();
    }
}
