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

import java.util.List;
import org.apache.maven.project.MavenProject;

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
}
