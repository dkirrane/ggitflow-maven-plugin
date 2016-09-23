<#macro fg color>${'\x1B'}[${color}m</#macro>
<#macro bold>${'\x1B'}[1m</#macro>
<#macro reset>${'\x1B'}[0m</#macro>

<#assign header = header + ":">
<@fg 31/><@bold/>=====${""?right_pad(header?length, "=")}=====<@reset/>
<@fg 31/><@bold/>===  ${header?right_pad(header?length)}  ===<@reset/>
<@fg 31/><@bold/>=====${""?right_pad(header?length, "=")}=====<@reset/>

<@fg 31/><@bold/>${message}<@reset/>

