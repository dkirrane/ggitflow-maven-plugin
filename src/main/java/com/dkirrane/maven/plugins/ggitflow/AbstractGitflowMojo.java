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

import com.dkirrane.maven.plugins.ggitflow.prompt.Prompter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
//import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 */
public class AbstractGitflowMojo extends AbstractMojo {

    @Parameter(property = "msgPrefix", defaultValue = "")
    protected String msgPrefix;

    @Parameter(property = "msgSuffix", defaultValue = "")
    protected String msgSuffix;

    @Requirement(role = Prompter.class, hint = "prompter", optional = false)
    protected Prompter prompter;

//    /**
//     * @parameter property="plugin"
//     * @required
//     */
//    @Component
//    protected PluginDescriptor pluginDescriptor;
//
    /**
     * The project currently being build.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    @Component
    protected MavenProject project;

    /**
     * The current Maven session.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    @Component
    protected MavenSession session;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    @Component
    protected BuildPluginManager pluginManager;

    protected final MavenProject getProject() {
        return project;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (null == project) {
            throw new NullPointerException("MavenProject is null");
        } else {
            getLog().info("Gitflow pom  '" + project.getBasedir() + "'");
        }
    }

    public String getMsgPrefix() {
        return (StringUtils.isBlank(msgPrefix)) ? "" : msgPrefix + " ";
    }

    public String getMsgSuffix() {
        return (StringUtils.isBlank(msgSuffix)) ? "" : " " + msgSuffix;
    }
}
