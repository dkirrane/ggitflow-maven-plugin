<#macro fg color>${'\x1B'}[${color}m</#macro>
<#macro bold>${'\x1B'}[1m</#macro>
<#macro reset>${'\x1B'}[0m</#macro>

<#assign header = header + ":">
<@fg 34/><@bold/>=====${""?right_pad(header?length, "=")}=====<@reset/>
<@fg 34/><@bold/>===  ${header?right_pad(header?length)}  ===<@reset/>
<@fg 34/><@bold/>=====${""?right_pad(header?length, "=")}=====<@reset/>

<@fg 34/><@bold/>Pushing:<@reset/>
<#if pushTags?has_content>
<#list pushTags as tag>
    <@fg 34/><@bold/>- Tag ${tag}<@reset/>
</#list>
</#if>
<#if pushBranches?has_content>
<#list pushBranches as branch>
    <@fg 34/><@bold/>- ${branch}<@reset/>
</#list>
</#if>

<#if deleteBranches?has_content>
<@fg 34/><@bold/>Deleting:<@reset/>
<#list deleteBranches as branch>
    <@fg 34/><@bold/>- ${branch}<@reset/>
</#list>
</#if>

