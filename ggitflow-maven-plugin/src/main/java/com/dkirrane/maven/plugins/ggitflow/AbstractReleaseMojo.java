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

import static com.dkirrane.gitflow.groovy.Constants.DEFAULT_RELEASE_BRN_PREFIX;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.hudson.util.GenericArtifactVersion;

/**
 *
 */
public class AbstractReleaseMojo extends AbstractGitflowMojo {

    @Parameter(property = "releaseName")
    protected String releaseName;

    @Parameter(defaultValue = "false", property = "pushReleases")
    private boolean pushReleases = false;

    public String getReleaseBranchPrefix() {
        String prefix = getGitflowInit().getReleaseBranchPrefix();
        if (StringUtils.isBlank(prefix)) {
            prefix = DEFAULT_RELEASE_BRN_PREFIX;
        }
        return prefix;
    }

    public String getNextDevelopmentVersion(String version) throws MojoFailureException {
        getLog().info("Project version '" + version + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(version);
        GenericArtifactVersion nextDevelopVersion = artifactVersion.upgradeLeastSignificantNumber();

        getLog().info("Project version '" + nextDevelopVersion + "'");
        return nextDevelopVersion.toString();
    }
}
