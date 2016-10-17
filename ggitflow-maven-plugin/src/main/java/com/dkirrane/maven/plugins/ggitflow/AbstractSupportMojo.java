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

import static com.dkirrane.gitflow.groovy.Constants.DEFAULT_SUPPORT_BRN_PREFIX;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.hudson.util.GenericArtifactVersion;
import static org.jfrog.hudson.util.GenericArtifactVersion.DEFAULT_VERSION_COMPONENT_SEPARATOR;
import static org.jfrog.hudson.util.GenericArtifactVersion.SNAPSHOT_QUALIFIER;

public class AbstractSupportMojo extends AbstractGitflowMojo {

    /**
     * The name for the support branch.
     *
     * @since 1.2
     */
    @Parameter(property = "supportName", required = false)
    protected String supportName;

    protected String getSupportBranchPrefix() {
        String prefix = getGitflowInit().getSupportBranchPrefix();
        if (StringUtils.isBlank(prefix)) {
            prefix = DEFAULT_SUPPORT_BRN_PREFIX;
        }
        return prefix;
    }

    protected String trimSupportName(String name) throws MojoFailureException {
        if (StringUtils.isBlank(name)) {
            throw new MojoFailureException("Missing argument <name>");
        }

        // remove whitespace
        name = name.replaceAll("\\s+", "");

        // trim off starting any leading 'support/' prefix
        String prefix = getSupportBranchPrefix();
        if (name.startsWith(prefix)) {
            name = name.substring(prefix.length());
        }

        return name;
    }

    protected String getSupportVersion(String currentVersion) throws MojoFailureException {
        getLog().debug("getSupportVersion from '" + currentVersion + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(currentVersion);

        StringBuilder sb = new StringBuilder(10);
        int pCount = artifactVersion.getPrimaryNumberCount();
        if (pCount < 3) {
            // Support versions should be like 1.0-xx e.g. 1.0-1, 1.0-2 etc
            sb.append(artifactVersion.getPrimaryNumbersAsString()).append('-').append("xx");
            sb.append(artifactVersion.getAnnotationAsString()).append(artifactVersion.getBuildSpecifierAsString());
        } else {
            // Support versions should be like 1.0.1-xx e.g. 1.0.1-1, 1.0.1-2 etc
            if (null != artifactVersion.getAnnotation()) {
                throw new MojoFailureException("Cannot start Support branch. Primary number " + artifactVersion.getPrimaryNumbersAsString() + " and annotations are already set " + artifactVersion.getAnnotation());
            }
            sb.append(artifactVersion.getPrimaryNumbersAsString()).append('-').append("xx");
        }

        return sb.toString();
    }

    protected String getSupportSnapshotVersion(String currentVersion) throws MojoFailureException {
        getLog().debug("getSupportSnapshotVersion from '" + currentVersion + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(currentVersion);

        StringBuilder sb = new StringBuilder(10);
        int pCount = artifactVersion.getPrimaryNumberCount();
        if (pCount < 3) {
            // Support versions should be like 1.0-xx e.g. 1.0-1, 1.0-2 etc
            sb.append(artifactVersion.getPrimaryNumbersAsString()).append('-').append("1");
            sb.append(artifactVersion.getAnnotationAsString());
        } else {
            // Support versions should be like 1.0.1-xx e.g. 1.0.1-1, 1.0.1-2 etc
            if (null != artifactVersion.getAnnotation()) {
                throw new MojoFailureException("Cannot start Support branch. Primary number " + artifactVersion.getPrimaryNumbersAsString() + " and annotations are already set " + artifactVersion.getAnnotation());
            }
            sb.append(artifactVersion.getPrimaryNumbersAsString()).append('-').append("1");
        }

        sb.append(DEFAULT_VERSION_COMPONENT_SEPARATOR).append(SNAPSHOT_QUALIFIER);

        return sb.toString();
    }

    protected String getNextSupportVersion(String currentVersion) throws MojoFailureException {
        getLog().debug("getNextSupportVersion from '" + currentVersion + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(currentVersion);

        StringBuilder sb = new StringBuilder(10);
        Integer annotationRevision = artifactVersion.getAnnotationRevision();
        if (null == annotationRevision) {
            throw new MojoFailureException("Cannot find Maven buildNumber in support version " + currentVersion);
        }
        GenericArtifactVersion upgradeAnnotationRevision = artifactVersion.upgradeAnnotationRevision();
        return upgradeAnnotationRevision.toString();
    }
}
