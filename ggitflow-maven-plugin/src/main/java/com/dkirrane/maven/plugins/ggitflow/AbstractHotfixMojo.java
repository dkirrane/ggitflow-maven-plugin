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

import static com.dkirrane.gitflow.groovy.Constants.DEFAULT_HOTFIX_BRN_PREFIX;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

public class AbstractHotfixMojo extends AbstractGitflowMojo {

    public String getHotfixBranchPrefix() {
        String prefix = getGitflowInit().getHotfixBranchPrefix();
        if (StringUtils.isBlank(prefix)) {
            prefix = DEFAULT_HOTFIX_BRN_PREFIX;
        }
        return prefix;
    }

    public String trimHotfixName(String name) throws MojoFailureException {
        if (StringUtils.isBlank(name)) {
            throw new MojoFailureException("Missing argument <featureName>");
        }

        // remove whitespace
        name = name.replaceAll("\\s+", "");

        // trim off starting any leading 'hotfix/' prefix
        String prefix = getHotfixBranchPrefix();
        if (name.startsWith(prefix)) {
            name = name.substring(prefix.length());
        }

        return name;
    }
}
