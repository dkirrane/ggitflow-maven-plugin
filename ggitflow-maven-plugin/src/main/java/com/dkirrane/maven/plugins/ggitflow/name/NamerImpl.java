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
package com.dkirrane.maven.plugins.ggitflow.name;

import java.util.regex.Pattern;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import static org.hamcrest.Matchers.matchesPattern;
import static org.jfrog.hudson.util.GenericArtifactVersion.VERSION_REGEX;
import static org.valid4j.Validation.validate;
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString;

/**
 * Default {@link Namer} implementation.
 *
 * @since 3.0
 */
@Component(role = Namer.class, instantiationStrategy = "per-lookup")
public class NamerImpl implements Namer {

    @Override
    public String trimRefName(String refName) {
        if (StringUtils.isBlank(refName)) {
            return "";
        }

        /* Valid Git ref name: https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html */
        return refName
                .replaceAll("\\.lock", "") // cannot end with the sequence .lock
                .replaceAll("^[.]+", "") // cannot begin with a dot .
                .replaceAll("/\\.", "") // cannot contain /.
                .replaceAll("\\.{2,}", "") // cannot have two consecutive dots .. anywhere
                .replaceAll("[\040\0177 ~^:]+", "") // cannot have ASCII control characters (i.e. bytes whose values are lower than \040, or \177 DEL), space, tilde ~, caret ^, or colon : anywhere
                .replaceAll("[?*\\[\\]]+", "") // cannot have question-mark ?, asterisk *, or open bracket [ anywhere
                .replaceAll("[/]{2,}", "/") // cannot contain multiple consecutive slashes /
                .replaceAll("^/+|/+$", "") // or cannot begin or end with a slash /
                .replaceAll("[.]+$", "") // cannot end with a dot .
                .replaceAll("@\\{", "") // cannot contain a sequence @{
                .replaceAll("@", "") // cannot be the single character @
                .replaceAll("\\\\", "") // cannot contain a \
                ;
    }

    @Override
    public String getBranchName(String prefix, String name, String version) {
        validate(prefix, notEmptyString(), new IllegalArgumentException("Argument prefix cannot be null or emtpy"));
        validate(version, notEmptyString(), new IllegalArgumentException("Argument version cannot be null or emtpy"));
        validate(version, matchesPattern(VERSION_REGEX), new IllegalArgumentException("Argument is not a valid maven verison: " + version));

        String branchName;
        if (StringUtils.isBlank(name)) {
            branchName = prefix + version; // just use Gitflwo branch prefix and version
        } else {
            name = name.replaceFirst("^" + Pattern.quote(prefix), "");
            if (name.equals(version)) {
                branchName = prefix + name;
            } else {
                branchName = prefix + name + '/' + version;
            }
        }
        return branchName;
    }

}
