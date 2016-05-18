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

import org.apache.maven.plugins.annotations.Parameter;

public class AbstractHotfixMojo extends AbstractGitflowMojo {

    /**
     * If <code>true</code>, the hotfix branch is pushed to the remote
     * repository
     *
     * @since 1.6
     */
    @Parameter(property = "pushHotfixBranch", defaultValue = "false", required = false)
    protected boolean pushHotfixBranch;

}
