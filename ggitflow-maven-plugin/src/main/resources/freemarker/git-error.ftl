<#macro fg color>${'\x1B'}[${color}m</#macro>
<#macro bold>${'\x1B'}[1m</#macro>
<#macro reset>${'\x1B'}[0m</#macro>

<#assign header = header + ":">
<@fg 31/><@bold/>=====${""?right_pad(header?length, "=")}=====<@reset/>
<@fg 31/><@bold/>===  ${header?right_pad(header?length)}  ===<@reset/>
<@fg 31/><@bold/>=====${""?right_pad(header?length, "=")}=====<@reset/>

<#if exitCode??>
    <@fg 31/><@bold/>Git exited code: ${exitCode}<@reset/>
</#if>

<#if message??>
    <@fg 31/><@bold/>Message: ${message}<@reset/>
</#if>

<#-- 
<#if stout??>
    <@fg 31/><@bold/>Stout: ${stout}<@reset/>
</#if>

<#if sterr??>
    <@fg 31/><@bold/>Sterr: ${sterr}<@reset/>
</#if>
-->

