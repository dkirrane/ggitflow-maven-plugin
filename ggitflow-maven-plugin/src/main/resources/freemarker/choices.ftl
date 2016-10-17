<#macro fg color>${'\x1B'}[${color}m</#macro>
<#macro bold>${'\x1B'}[1m</#macro>
<#macro reset>${'\x1B'}[0m</#macro>

<#assign header = header + ":">
<@fg 34/><@bold/>=====${""?right_pad(header?length, "=")}=====<@reset/>
<@fg 34/><@bold/>===  ${header?right_pad(header?length)}  ===<@reset/>
<@fg 34/><@bold/>=====${""?right_pad(header?length, "=")}=====<@reset/>
<#assign y = 0>
<#list choices?chunk(5) as row>
  <#list row as cell><#assign y++><@fg 34/><@bold/>${"(${y})"?right_pad(5)}<@reset/>${cell?right_pad(30)}</#list>
</#list>
