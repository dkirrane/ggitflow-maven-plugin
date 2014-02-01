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
 *//*
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

import com.dkirrane.gitflow.groovy.Constants;

/**
 *
 */
public class Prefixes {

    private String masterBranch;
    private String developBranch;
    private String featureBranchPrefix;
    private String releaseBranchPrefix;
    private String hotfixBranchPrefix;
    private String supportBranchPrefix;
    private String versionTagPrefix;

    public Prefixes() {
        this.masterBranch = Constants.DEFAULT_MASTER_BRN_NAME;
        this.developBranch = Constants.DEFAULT_DEVELOP_BRN_NAME;
        this.featureBranchPrefix = Constants.DEFAULT_FEATURE_BRN_PREFIX;
        this.releaseBranchPrefix = Constants.DEFAULT_RELEASE_BRN_PREFIX;
        this.hotfixBranchPrefix = Constants.DEFAULT_HOTFIX_BRN_PREFIX;
        this.supportBranchPrefix = Constants.DEFAULT_SUPPORT_BRN_PREFIX;
        this.versionTagPrefix = Constants.DEFAULT_VERSION_TAG_PREFIX;
    }

    public String getMasterBranch() {
        return masterBranch;
    }

    public void setMasterBranch(String masterBranch) {
        this.masterBranch = masterBranch;
    }

    public String getDevelopBranch() {
        return developBranch;
    }

    public void setDevelopBranch(String developBranch) {
        this.developBranch = developBranch;
    }

    public String getFeatureBranchPrefix() {
        return featureBranchPrefix;
    }

    public void setFeatureBranchPrefix(String featureBranchPrefix) {
        this.featureBranchPrefix = featureBranchPrefix;
    }

    public String getReleaseBranchPrefix() {
        return releaseBranchPrefix;
    }

    public void setReleaseBranchPrefix(String releaseBranchPrefix) {
        this.releaseBranchPrefix = releaseBranchPrefix;
    }

    public String getHotfixBranchPrefix() {
        return hotfixBranchPrefix;
    }

    public void setHotfixBranchPrefix(String hotfixBranchPrefix) {
        this.hotfixBranchPrefix = hotfixBranchPrefix;
    }

    public String getSupportBranchPrefix() {
        return supportBranchPrefix;
    }

    public void setSupportBranchPrefix(String supportBranchPrefix) {
        this.supportBranchPrefix = supportBranchPrefix;
    }

    public String getVersionTagPrefix() {
        return versionTagPrefix;
    }

    public void setVersionTagPrefix(String versionTagPrefix) {
        this.versionTagPrefix = versionTagPrefix;
    }

}
