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


import static com.dkirrane.gitflow.groovy.Constants.DEFAULT_FEATURE_BRN_PREFIX;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.hudson.util.GenericArtifactVersion;

/**
 *
 */
public class AbstractFeatureMojo extends AbstractGitflowMojo {

    @Parameter(property = "featureName")
    protected String featureName;

    @Parameter(defaultValue = "false", property = "enableFeatureVersions")
    protected boolean enableFeatureVersions = false;

    @Parameter(defaultValue = "false", property = "pushFeatures")
    protected boolean pushFeatures = false;   

    public String getFeatureBranchPrefix() {
        String prefix = getGitflowInit().getFeatureBranchPrefix();
        if (StringUtils.isBlank(prefix)) {
            prefix = DEFAULT_FEATURE_BRN_PREFIX;
        }
        return prefix;
    }

    public String getFeatureName(String featureLabel) throws MojoFailureException {
        if (StringUtils.isBlank(featureLabel)) {
            throw new MojoFailureException("Missing argument <featureName>");
        }

        featureLabel = featureLabel.replaceAll("\\s+", "");
//        featureLabel = featureLabel.replaceAll("-+", "_");
        return featureLabel;
    }

    public String getFeatureVersion(String version, String featureLabel) throws MojoFailureException {
        getLog().info("Project version '" + version + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(version);
        String primaryNumbersAsString = artifactVersion.getPrimaryNumbersAsString();
        String annotationAsString = artifactVersion.getAnnotationAsString();
        Character annotationSeparator = artifactVersion.getAnnotationRevisionSeparator();
        String buildSpecifier = artifactVersion.getBuildSpecifier();
        Character buildSpecifierSeparator = artifactVersion.getBuildSpecifierSeparator();

        getLog().info("Parsed version = " + artifactVersion.toString());
        if (!StringUtils.isBlank(annotationAsString)) {
            throw new MojoFailureException("Cannot add feature name to version. An annotation " + annotationAsString + " already exists");
        }

        final StringBuilder result = new StringBuilder(30);

        result.append(primaryNumbersAsString).append(annotationSeparator).append(featureLabel);

        if (buildSpecifier != null) {
            if (buildSpecifierSeparator != null) {
                result.append(buildSpecifierSeparator);
            }
            result.append(buildSpecifier);
        }

        return result.toString();
    }

    public String getNonFeatureVersion(String version, String featureLabel) {
        getLog().info("Project version '" + version + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(version);
        String primaryNumbersAsString = artifactVersion.getPrimaryNumbersAsString();
        String annotationAsString = artifactVersion.getAnnotationAsString();
        String buildSpecifier = artifactVersion.getBuildSpecifier();
        Character buildSpecifierSeparator = artifactVersion.getBuildSpecifierSeparator();

        getLog().info("Parsed version = " + artifactVersion.toString());
        if (StringUtils.isBlank(annotationAsString)) {
            getLog().warn("Cannot remove feature name from pom version. The version annotation does not exists");
            return version;
        }

        if (('-' + featureLabel).equals(annotationAsString)) {
            final StringBuilder result = new StringBuilder(30);
            result.append(primaryNumbersAsString);

            if (buildSpecifier != null) {
                if (buildSpecifierSeparator != null) {
                    result.append(buildSpecifierSeparator);
                }
                result.append(buildSpecifier);
            }

            return result.toString();
        } else {
            getLog().warn("Cannot remove feature name from pom version. The version annotation [" + annotationAsString + "] does not match the feature label [" + featureLabel + "]");
            return version;
        }
    }
}
