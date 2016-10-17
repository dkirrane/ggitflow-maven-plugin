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
import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 * @author dkirrane
 */
public interface ExceptionMapper {

    void setRepoDir(File repoDir) throws MojoFailureException;

    void handle(String header, GitCommandException gce) throws MojoFailureException;

    void handle(String header, GitflowException gfe) throws MojoFailureException;

    void handle(String header, GitflowMergeConflictException gmce) throws MojoFailureException;

    void handle(MojoFailureException mfe) throws MojoFailureException;

    void handle(MojoExecutionException mee) throws MojoExecutionException;
}
