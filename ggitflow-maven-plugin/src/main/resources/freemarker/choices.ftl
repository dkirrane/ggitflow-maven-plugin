<#macro fg color>${'\x1B'}[${color + 32}m</#macro>
<#macro bold>${'\x1B'}[1m</#macro>
<#macro reset>${'\x1B'}[0m</#macro>

<#assign header = header + ":">
<@fg 4/><@bold/>=====${""?right_pad(header?length, "=")}=====
<@fg 4/><@bold/>===  ${header?right_pad(header?length)}  ===
<@fg 4/><@bold/>=====${""?right_pad(header?length, "=")}=====
<#assign y = 0>
<#list choices?chunk(5) as row>
  <#list row as cell><#assign y++><@fg 4/><@bold/>${"(${y})"?right_pad(5)}<@reset/>${cell?right_pad(30)}</#list>
</#list>
