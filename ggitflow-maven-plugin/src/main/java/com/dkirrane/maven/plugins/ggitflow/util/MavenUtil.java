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
package com.dkirrane.maven.plugins.ggitflow.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 *
 */
public final class MavenUtil {

    public static MavenProject getRootProject(List<MavenProject> reactorProjects) {
        MavenProject project = reactorProjects.get(0);
        for (MavenProject currentProject : reactorProjects) {
            if (currentProject.isExecutionRoot()) {
                project = currentProject;
                break;
            }
        }
        return project;
    }

    public static Model readPom(List<MavenProject> reactorProjects) throws MojoExecutionException {
        MavenProject rootProject = getRootProject(reactorProjects);
        File pomFile = rootProject.getFile();
        
        FileReader fileReader = null;

        try {
            fileReader = new FileReader(pomFile);
            MavenXpp3Reader m = new MavenXpp3Reader();
            Model model = m.read(fileReader);
            return model;
        } catch (FileNotFoundException ex) {
            throw new MojoExecutionException("POM " + pomFile.getAbsolutePath() + " could not be parsed.", ex);
        } catch (IOException ex) {
            throw new MojoExecutionException("POM " + pomFile.getAbsolutePath() + " could not be parsed.", ex);
        } catch (XmlPullParserException ex) {
            throw new MojoExecutionException("POM " + pomFile.getAbsolutePath() + " could not be parsed.", ex);
        } finally {
            if (null != fileReader) {
                try {
                    fileReader.close();
                } catch (IOException ex) {
                }
            }
        }
    }
}
