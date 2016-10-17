/*
 * Copyright 2016 dkirrane.
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
package com.dkirrane.maven.plugins.ggitflow.ex;

import com.dkirrane.gitflow.groovy.ex.GitCommandException;
import com.dkirrane.gitflow.groovy.ex.GitflowException;
import com.dkirrane.gitflow.groovy.ex.GitflowMergeConflictException;
import com.dkirrane.maven.plugins.ggitflow.prompt.FreeMarker;
import java.io.File;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 *
 * @author dkirrane
 */
@Component(role = ExceptionMapper.class, instantiationStrategy = "per-lookup")
public class ExceptionMapperImpl implements ExceptionMapper {

    @Requirement(role = FreeMarker.class)
    protected FreeMarker freeMarker;

    private File repoDir;

    @Override
    public void setRepoDir(File repoDir) throws MojoFailureException {
        this.repoDir = repoDir;
    }

    @Override
    public void handle(String header, GitCommandException gce) throws MojoFailureException {
        freeMarker.logGitError(header, gce.getMessage(), gce.getExitCode(), gce.getStout(), gce.getSterr());
        throw new MojoFailureException(header);
    }

    @Override
    public void handle(String header, GitflowException gfe) throws MojoFailureException {
        freeMarker.logError(header, gfe.getMessage());
        throw new MojoFailureException(header);
    }

    @Override
    public void handle(String header, GitflowMergeConflictException gmce) throws MojoFailureException {
        String gitDir = (null == repoDir) ? ".git" : FilenameUtils.separatorsToUnix(repoDir.getPath()) + "/.git";
        String footer = "Resolve the merge conflict & re-run: \n\tgit mergetool\n\tgit commit --all --file " + gitDir + "/MERGE_MSG\n\tRe-run release-finish";
        freeMarker.logMergeConflict(header, footer, gmce.getMessage(), gmce.getConflictedFiles());
        throw new MojoFailureException(header);
    }

    @Override
    public void handle(MojoFailureException mfe) throws MojoFailureException {
        freeMarker.logError("Error", mfe.getMessage());
        throw mfe;
    }

    @Override
    public void handle(MojoExecutionException mee) throws MojoExecutionException {
        freeMarker.logError("Error", mee.getMessage());
        throw mee;
    }

}
